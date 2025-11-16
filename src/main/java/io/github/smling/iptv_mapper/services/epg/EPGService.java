package io.github.smling.iptv_mapper.services.epg;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.smling.iptv_mapper.EPGClient;
import io.github.smling.iptv_mapper.UriChecker;
import io.github.smling.iptv_mapper.factories.XmlMapperFactory;
import io.github.smling.iptv_mapper.models.DataSourceType;
import io.github.smling.iptv_mapper.models.dao.DataSourceEntity;
import io.github.smling.iptv_mapper.models.dao.epg.*;
import io.github.smling.iptv_mapper.models.dto.epg.*;
import io.github.smling.iptv_mapper.parsers.EPGTimeParser;
import io.github.smling.iptv_mapper.repositories.DataSourceRepository;
import io.github.smling.iptv_mapper.repositories.epg.*;
import io.github.smling.iptv_mapper.services.IngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EPGService extends IngestService {
    private static final Logger logger = LoggerFactory.getLogger(EPGService.class);
    private final Clock clock;
    private final TvRepository tvRepository;
    private final ChannelRepository channelRepository;
    private final ProgrammeRepository programmeRepository;
    private final ChannelDisplayNameRepository channelDisplayNameRepository;
    private final ChannelUrlRepository channelUrlRepository;
    private final String appName;
    private final Environment environment;

    public EPGService(DataSourceRepository dataSourceRepo,
                      Clock clock,
                      TvRepository tvRepository,
                      ChannelRepository channelRepository,
                      ProgrammeRepository programmeRepository,
                      ChannelDisplayNameRepository channelDisplayNameRepository,
                      ChannelUrlRepository channelUrlRepository,
                      @Value("${spring.application.name}") String appName, Environment environment,
                      org.springframework.context.ApplicationContext applicationContext
    ) {
        super(dataSourceRepo, applicationContext);
        this.clock = clock;
        this.tvRepository = tvRepository;
        this.channelRepository = channelRepository;
        this.programmeRepository = programmeRepository;
        this.channelDisplayNameRepository = channelDisplayNameRepository;
        this.channelUrlRepository = channelUrlRepository;
        this.appName = appName;
        this.environment = environment;
    }

    @Override
    public DataSourceType getDataSourcesType() {
        return DataSourceType.EPG;
    }

    @Override
    public void ingestOneInTx(DataSourceEntity ds) {
        URI requestUri = URI.create(ds.getUrl());
        var urlCheck = new UriChecker().check(requestUri);
        if(!urlCheck.isReachable()) {
            logger.debug("⏭️ Skip data source {} (type={}, enabled={}, check={} ms)", ds.getId(), ds.getType(), ds.isEnabled(), urlCheck.responseTimeMillis());
            return; // do not attempt fetch if not reachable
        }

        logger.info("📡 Starting EPG ingest for {}", requestUri);
        try {
            // Download first to avoid premature EOF when parsing is slow due to DB writes
            java.nio.file.Path tmp = new EPGClient().downloadToTempFile(requestUri);
            logger.debug("💾 EPG downloaded to temp file: {}", tmp);
            // Some providers concatenate multiple XML documents and include repeated XML declarations.
            // Stream-sanitize by removing BOMs and any lines starting with an XML declaration, then wrap in a single synthetic root.
            java.nio.file.Path sanitizedFile = java.nio.file.Files.createTempFile("epg-sanitized-", ".xml");
            long lines = 0L;
            long droppedXmlDecl = 0L;
            try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(tmp, StandardCharsets.UTF_8);
                 java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(sanitizedFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines++;
                    String noBom = line.replace("\uFEFF", "");
                    String trimmed = noBom.stripLeading();
                    if (trimmed.startsWith("<?xml")) {
                        droppedXmlDecl++;
                        continue; // drop XML declarations found mid-stream
                    }
                    writer.write(noBom);
                    writer.newLine();
                }
            }
            try {
                long rawSize = java.nio.file.Files.size(tmp);
                long sanitizedSize = java.nio.file.Files.size(sanitizedFile);
                logger.debug("🧽 Sanitized XML: lines={}, droppedDecls={}, rawSize={} bytes, sanitizedSize={} bytes", lines, droppedXmlDecl, rawSize, sanitizedSize);
            } catch (Exception ignore) {}
            byte[] prefix = "<root>".getBytes(StandardCharsets.UTF_8);
            byte[] suffix = "</root>".getBytes(StandardCharsets.UTF_8);
            try (java.io.InputStream original = java.nio.file.Files.newInputStream(sanitizedFile);
                 java.io.InputStream wrapped = new java.io.SequenceInputStream(
                         new java.io.ByteArrayInputStream(prefix),
                         new java.io.SequenceInputStream(original, new java.io.ByteArrayInputStream(suffix)))) {
                streamIngest(wrapped, ds, urlCheck);
                logger.info("✅ Completed EPG ingest for {}", requestUri);
            } finally {
                try { java.nio.file.Files.deleteIfExists(tmp); } catch (Exception ignore) {}
                try { java.nio.file.Files.deleteIfExists(sanitizedFile); } catch (Exception ignore) {}
            }
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("🛑 Interrupted while fetching {}", requestUri, e);
        } catch(IOException e) {
            logger.error("❌ I/O error occurred while fetching {}", requestUri, e);
        }
    }

    private void streamIngest(InputStream in, DataSourceEntity ds, UriChecker.UrlCheckResult urlCheck) {
        XMLInputFactory f = XMLInputFactory.newFactory();
        XMLStreamReader r = null;
        try {
            r = f.createXMLStreamReader(in);

            TvEntity tvEntity = null;
            String tvGenName;
            String tvGenUrl;
            Map<String, ChannelEntity> channelCache = new HashMap<>();
            int chCreated = 0, chUpdated = 0, progCreated = 0, progUpdated = 0;

            long startedNs = System.nanoTime();
            int chSeen = 0, progSeen = 0;
            logger.debug("📥 streamIngest start: ds={}, urlCheck={} {}ms", ds.getId(), urlCheck.status(), urlCheck.responseTimeMillis());

            while (r.hasNext()) {
                int ev = r.next();
                if (ev != XMLStreamConstants.START_ELEMENT) continue;
                String name = r.getLocalName();

                if ("tv".equals(name)) {
                    tvGenName = attr(r, "generator-info-name");
                    tvGenUrl  = attr(r, "generator-info-url");
                    final String genName = tvGenName;
                    final String genUrl  = tvGenUrl;
                    final long urlCheckMs = urlCheck.responseTimeMillis();
                    tvEntity  = tvRepository
                            .findByDataSource_Id(ds.getId())
                            .orElseGet(() -> {
                                logger.debug("🆕 Created TvEntity gen='{}' url='{}' ({} ms)", genName, genUrl, urlCheckMs);
                                return TvEntity.of(ds, new Tv(null, null, null, null, genName, genUrl, List.of(), List.of()), clock);
                            });

                    // Update checker timing on tv record
                    tvEntity.setUrlCheckerResult(urlCheck.status().name())
                            .setUrlCheckerMs(urlCheck.responseTimeMillis())
                    ;
                    tvEntity = tvRepository.save(tvEntity);
                } else if ("channel".equals(name)) {
                    chSeen++;
                    if (tvEntity == null) continue; // safeguard
                    String channelId = attr(r, "id");
                    String displayName = null;
                    String displayNameLang = null;
                    List<UrlRef> urlRefs = new java.util.ArrayList<>();

                    // descend until end of channel
                    while (r.hasNext()) {
                        int inner = r.next();
                        logger.trace("Current Xml element attribute: {}", inner);
                        if (inner == XMLStreamConstants.START_ELEMENT && "display-name".equals(r.getLocalName())) {
                            displayNameLang = attr(r, "lang");
                            displayName = text(r);
                        } else if (inner == XMLStreamConstants.START_ELEMENT && "url".equals(r.getLocalName())) {
                            String sys = attr(r, "system");
                            String val = text(r);
                            urlRefs.add(new UrlRef(sys, val));
                        } else if (inner == XMLStreamConstants.END_ELEMENT && "channel".equals(r.getLocalName())) {
                            break;
                        }
                    }
                    // Build DTO first for clearer downstream logic
                    Channel chDto = new Channel(
                            channelId,
                            (displayName == null ? List.of() : List.of(new Text(displayNameLang, displayName))),
                            List.of(),
                            urlRefs
                    );
                    boolean needUpdate = false;
                    String finalDisplayName = displayName;
                    TvEntity finalTvEntity = tvEntity;
                    ChannelEntity chEntity = channelRepository.findByTv_IdAndChannelId(tvEntity.getId(), channelId)
                            .or(() -> channelRepository.findByChannelId(channelId))
                            .orElseGet(() ->{
                                logger.debug("➕ Inserted channel '{}' ('{}')", channelId, finalDisplayName);
                                return channelRepository.save(ChannelEntity.of(finalTvEntity, chDto));
                            })
                            ;
                    // Derive newName from DTO
                    String newName = (!chDto.displayNames().isEmpty() ? chDto.displayNames().getFirst().value() : null);
                    if (!Objects.equals(chEntity.getDisplayName(), newName) && newName != null) {
                        chEntity.setDisplayName(newName);
                        needUpdate = true;
                        logger.debug("✏️ Updated channel '{}' displayName -> '{}'", channelId, newName);
                    }
                    // sync related display-names based on DTO using repositories (avoid lazy init)
                    if (!chDto.displayNames().isEmpty()) {
                        var existingDns = channelDisplayNameRepository.findByChannel_IdOrderByPositionAsc(chEntity.getId());
                        boolean same = existingDns.size() == chDto.displayNames().size();
                        if (same) {
                            for (int i = 0; i < existingDns.size(); i++) {
                                var e = existingDns.get(i);
                                var t = chDto.displayNames().get(i);
                                if (!Objects.equals(e.getLang(), t.lang()) || !Objects.equals(e.getName(), t.value())) { same = false; break; }
                            }
                        }
                        if (!same) {
                            channelDisplayNameRepository.deleteByChannel_Id(chEntity.getId());
                            int pos = 0;
                            java.util.List<ChannelDisplayNameEntity> items = new java.util.ArrayList<>();
                            for (Text t : chDto.displayNames()) {
                                items.add(new ChannelDisplayNameEntity()
                                        .setChannel(chEntity)
                                        .setLang(t.lang())
                                        .setName(t.value())
                                        .setPosition(pos++));
                            }
                            channelDisplayNameRepository.saveAll(items);
                        }
                    }
                    // sync urls based on DTO using repository
                    if (!chDto.urls().isEmpty()) {
                        var existingUrls = channelUrlRepository.findByChannel_Id(chEntity.getId());
                        boolean same = existingUrls.size() == chDto.urls().size();
                        if (same) {
                            for (int i = 0; i < existingUrls.size(); i++) {
                                var e = existingUrls.get(i);
                                var u = chDto.urls().get(i);
                                if (!Objects.equals(e.getSystem(), u.system()) || !Objects.equals(e.getUrl(), u.value())) { same = false; break; }
                            }
                        }
                        if (!same) {
                            channelUrlRepository.deleteByChannel_Id(chEntity.getId());
                            java.util.List<ChannelUrlEntity> items = new java.util.ArrayList<>();
                            for (UrlRef u : chDto.urls()) {
                                items.add(new ChannelUrlEntity()
                                        .setChannel(chEntity)
                                        .setSystem(u.system())
                                        .setUrl(u.value()));
                            }
                            channelUrlRepository.saveAll(items);
                        }
                    }
                    if (needUpdate) {
                        channelRepository.save(chEntity);
                        chUpdated++;
                    }
                    channelCache.put(channelId, chEntity);
                    if (chSeen % 50 == 0) {
                        long ms = (System.nanoTime() - startedNs) / 1_000_000;
                        logger.debug("📊 Channels seen={}, created={}, updated={}, elapsed={}ms", chSeen, chCreated, chUpdated, ms);
                    }
                } else if ("programme".equals(name)) {
                    progSeen++;
                    if (progSeen % 1000 == 0) {
                        long ms = (System.nanoTime() - startedNs) / 1_000_000;
                        logger.debug("📊 EPG programme screening: seen={}, created={}, updated={}, elapsed={}ms", progSeen, progCreated, progUpdated, ms);
                    }
                    String start = attr(r, "start");
                    String stop  = attr(r, "stop");
                    String chRef = attr(r, "channel");
                    // Build DTO pieces while traversing XML to avoid missing fields
                    java.util.List<Text> titles = new java.util.ArrayList<>();
                    java.util.List<Text> subTitles = new java.util.ArrayList<>();
                    java.util.List<Text> descs = new java.util.ArrayList<>();
                    String date = null;
                    java.util.List<Text> categories = new java.util.ArrayList<>();
                    java.util.List<Text> keywords = new java.util.ArrayList<>();
                    java.util.List<Text> languages = new java.util.ArrayList<>();
                    Text origLanguage = null;
                    ProgrammeLength length = null;
                    java.util.List<Icon> icons = new java.util.ArrayList<>();
                    java.util.List<UrlRef> urls = new java.util.ArrayList<>();
                    java.util.List<String> countries = new java.util.ArrayList<>();
                    java.util.List<EpisodeNumber> episodeNums = new java.util.ArrayList<>();
                    Video video = null;
                    Audio audio = null;
                    PreviouslyShown previouslyShown = null;
                    Text premiere = null;
                    Text lastChance = null;
                    Empty isNew = null;
                    java.util.List<Subtitles> subtitles = new java.util.ArrayList<>();
                    java.util.List<Rating> ratings = new java.util.ArrayList<>();
                    java.util.List<StarRating> starRatings = new java.util.ArrayList<>();
                    java.util.List<Review> reviews = new java.util.ArrayList<>();
                    java.util.List<Image> images = new java.util.ArrayList<>();

                    while (r.hasNext()) {
                        int inner = r.next();
                        if (inner == XMLStreamConstants.START_ELEMENT) {
                            String ln = r.getLocalName();
                            if ("title".equals(ln)) {
                                titles.add(new Text(attr(r, "lang"), text(r)));
                            } else if ("sub-title".equals(ln)) {
                                subTitles.add(new Text(attr(r, "lang"), text(r)));
                            } else if ("desc".equals(ln)) {
                                descs.add(new Text(attr(r, "lang"), text(r)));
                            } else if ("date".equals(ln)) {
                                date = text(r);
                            } else if ("category".equals(ln)) {
                                categories.add(new Text(attr(r, "lang"), text(r)));
                            } else if ("keyword".equals(ln)) {
                                keywords.add(new Text(attr(r, "lang"), text(r)));
                            } else if ("language".equals(ln)) {
                                languages.add(new Text(attr(r, "lang"), text(r)));
                            } else if ("orig-language".equals(ln)) {
                                origLanguage = new Text(attr(r, "lang"), text(r));
                            } else if ("length".equals(ln)) {
                                length = new ProgrammeLength(attr(r, "units"), text(r));
                            } else if ("icon".equals(ln)) {
                                icons.add(new Icon(attr(r, "src"), attr(r, "width"), attr(r, "height")));
                            } else if ("url".equals(ln)) {
                                urls.add(new UrlRef(attr(r, "system"), text(r)));
                            } else if ("country".equals(ln)) {
                                countries.add(text(r));
                            } else if ("episode-num".equals(ln)) {
                                episodeNums.add(new EpisodeNumber(attr(r, "system"), text(r)));
                            } else if ("video".equals(ln)) {
                                video = parseVideo(r);
                            } else if ("audio".equals(ln)) {
                                audio = parseAudio(r);
                            } else if ("previously-shown".equals(ln)) {
                                previouslyShown = new PreviouslyShown(attr(r, "start"), attr(r, "channel"));
                            } else if ("premiere".equals(ln)) {
                                premiere = new Text(attr(r, "lang"), text(r));
                            } else if ("last-chance".equals(ln)) {
                                lastChance = new Text(attr(r, "lang"), text(r));
                            } else if ("new".equals(ln)) {
                                isNew = new Empty();
                            } else if ("subtitles".equals(ln)) {
                                subtitles.add(parseSubtitles(r));
                            } else if ("rating".equals(ln)) {
                                ratings.add(parseRating(r));
                            } else if ("star-rating".equals(ln)) {
                                starRatings.add(parseStarRating(r));
                            } else if ("review".equals(ln)) {
                                reviews.add(parseReview(r));
                            } else if ("image".equals(ln)) {
                                images.add(parseImage(r));
                            }
                        } else if (inner == XMLStreamConstants.END_ELEMENT && "programme".equals(r.getLocalName())) {
                            break;
                        }
                    }

                    ChannelEntity chEntity = channelCache.get(chRef);
                    if (chEntity == null && tvEntity != null) {
                        chEntity = channelRepository.findByTv_IdAndChannelId(tvEntity.getId(), chRef)
                                .or(() -> channelRepository.findByChannelId(chRef))
                                .orElse(null);
                        if (chEntity != null) channelCache.put(chRef, chEntity);
                    }
                    if (chEntity == null) continue; // no channel mapping

                    var startTs = EPGTimeParser.parse(start);
                    var stopTs  = EPGTimeParser.parse(stop);
                    // Build DTO from parsed values
                    Programme pDto = new Programme(
                            start, stop, chRef,
                            null, null, null, null, null,
                            titles,
                            subTitles,
                            descs,
                            null,
                            date,
                            categories,
                            keywords,
                            languages,
                            origLanguage,
                            length,
                            icons,
                            urls,
                            countries,
                            episodeNums,
                            video,
                            audio,
                            previouslyShown,
                            premiere,
                            lastChance,
                            isNew,
                            subtitles,
                            ratings,
                            starRatings,
                            reviews,
                            images,
                            null);
                    var existingProg = programmeRepository
                            .findByChannel_IdAndStartTimeAndStopTime(chEntity.getId(), startTs, stopTs);
                    if (existingProg.isEmpty()) {
                        String firstTitleForMatch = titles.isEmpty() ? null : titles.get(0).value();
                        if (firstTitleForMatch != null && !firstTitleForMatch.isBlank()) {
                            existingProg = programmeRepository
                                    .findByChannel_IdAndStartTimeAndStopTimeAndTitle(chEntity.getId(), startTs, stopTs, firstTitleForMatch);
                        }
                    }

                    if (existingProg.isPresent()) {
                        var prog = existingProg.get();
                        String firstTitle = titles.isEmpty() ? null : titles.get(0).value();
                        String firstDesc  = descs.isEmpty() ? null : descs.get(0).value();
                        boolean changed = false;
                        if (!Objects.equals(prog.getTitle(), firstTitle)) { prog.setTitle(firstTitle); changed = true; }
                        if (!Objects.equals(prog.getDescription(), firstDesc)) { prog.setDescription(firstDesc); changed = true; }
                        if (changed) { programmeRepository.save(prog); progUpdated++; }
                    } else {
                        programmeRepository.save(ProgrammeEntity.of(chEntity, pDto));
                        progCreated++;
                    }
                    if (progSeen % 1000 == 0) {
                        long ms = (System.nanoTime() - startedNs) / 1_000_000;
                        logger.debug("📈 Programmes seen={}, created={}, updated={}, elapsed={}ms", progSeen, progCreated, progUpdated, ms);
                    }
                }
            }
            long totalMs = (System.nanoTime() - startedNs) / 1_000_000;
            logger.info("📦 EPG ingest summary: channels seen={} (created={}, updated={}), programmes seen={} (created={}, updated={}), took={}ms",
                    chSeen, chCreated, chUpdated, progSeen, progCreated, progUpdated, totalMs);
        } catch (XMLStreamException e) {
            logger.error("📉 Failed to parse EPG stream (partial): {}", e.getMessage());
            throw new IllegalStateException("📉 Failed to parse EPG stream", e);
        } finally {
            if (r != null) try { r.close(); } catch (Exception ignored) {}
        }
    }

    private static String attr(XMLStreamReader r, String name) {
        String v = r.getAttributeValue(null, name);
        return v != null ? v : "";
    }

    private static String text(XMLStreamReader r) throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.CHARACTERS || ev == XMLStreamConstants.CDATA) {
                sb.append(r.getText());
            } else if (ev == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return sb.toString();
    }

    private static Video parseVideo(XMLStreamReader r) throws XMLStreamException {
        String present = null, colour = null, aspect = null, quality = null;
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                String ln = r.getLocalName();
                if ("present".equals(ln)) present = text(r);
                else if ("colour".equals(ln)) colour = text(r);
                else if ("aspect".equals(ln)) aspect = text(r);
                else if ("quality".equals(ln)) quality = text(r);
            } else if (ev == XMLStreamConstants.END_ELEMENT && "video".equals(r.getLocalName())) {
                break;
            }
        }
        return new Video(present, colour, aspect, quality);
    }

    private static Audio parseAudio(XMLStreamReader r) throws XMLStreamException {
        String present = null, stereo = null;
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                String ln = r.getLocalName();
                if ("present".equals(ln)) present = text(r);
                else if ("stereo".equals(ln)) stereo = text(r);
            } else if (ev == XMLStreamConstants.END_ELEMENT && "audio".equals(r.getLocalName())) {
                break;
            }
        }
        return new Audio(present, stereo);
    }

    private static Subtitles parseSubtitles(XMLStreamReader r) throws XMLStreamException {
        String type = attr(r, "type");
        java.util.List<Text> langs = new java.util.ArrayList<>();
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT && "language".equals(r.getLocalName())) {
                langs.add(new Text(attr(r, "lang"), text(r)));
            } else if (ev == XMLStreamConstants.END_ELEMENT && "subtitles".equals(r.getLocalName())) {
                break;
            }
        }
        return new Subtitles(type, langs);
    }

    private static Rating parseRating(XMLStreamReader r) throws XMLStreamException {
        String system = attr(r, "system");
        String value = null;
        java.util.List<Icon> icons = new java.util.ArrayList<>();
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                String ln = r.getLocalName();
                if ("value".equals(ln)) value = text(r);
                else if ("icon".equals(ln)) icons.add(new Icon(attr(r, "src"), attr(r, "width"), attr(r, "height")));
            } else if (ev == XMLStreamConstants.END_ELEMENT && "rating".equals(r.getLocalName())) {
                break;
            }
        }
        return new Rating(system, value, icons);
    }

    private static StarRating parseStarRating(XMLStreamReader r) throws XMLStreamException {
        String system = attr(r, "system");
        String value = null;
        java.util.List<Icon> icons = new java.util.ArrayList<>();
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                String ln = r.getLocalName();
                if ("value".equals(ln)) value = text(r);
                else if ("icon".equals(ln)) icons.add(new Icon(attr(r, "src"), attr(r, "width"), attr(r, "height")));
            } else if (ev == XMLStreamConstants.END_ELEMENT && "star-rating".equals(r.getLocalName())) {
                break;
            }
        }
        return new StarRating(system, value, icons);
    }

    private static Review parseReview(XMLStreamReader r) throws XMLStreamException {
        String type = attr(r, "type");
        String source = attr(r, "source");
        String reviewer = attr(r, "reviewer");
        String lang = attr(r, "lang");
        String value = text(r);
        return new Review(type, source, reviewer, lang, value);
    }

    private static Image parseImage(XMLStreamReader r) throws XMLStreamException {
        String type = attr(r, "type");
        String size = attr(r, "size");
        String orient = attr(r, "orient");
        String system = attr(r, "system");
        String value = text(r);
        return new Image(type, size, orient, system, value);
    }

    private void upsertChannelsAndProgrammes(Tv tv, TvEntity tvEntity) {
        if(tv.channels().isEmpty()) return;
        var channels = tv.channels();
        // Avoid parallel JPA writes within a single transaction; use sequential processing
        channels.stream().forEach(chDto -> {
            // Upsert Channel by (tv, channelId)
            var existingCh = channelRepository.findByTv_IdAndChannelId(tvEntity.getId(), chDto.id())
                    .or(() -> channelRepository.findByChannelId(chDto.id()));

            ChannelEntity chEntity;
            if (existingCh.isPresent()) {
                chEntity = existingCh.get();
                String newName = (chDto.displayNames() != null && !chDto.displayNames().isEmpty()) ? chDto.displayNames().get(0).value() : null;
                if (!Objects.equals(chEntity.getDisplayName(), newName)) {
                    chEntity.setDisplayName(newName);
                    channelRepository.save(chEntity);
                }
            } else {
                chEntity = channelRepository.save(ChannelEntity.of(tvEntity, chDto));
            }

            // Upsert Programmes for this channel
            upsertProgrammesForChannel(chDto, chEntity, tv.programmes());
        });
    }

    private void upsertProgrammesForChannel(io.github.smling.iptv_mapper.models.dto.epg.Channel chDto,
                                            ChannelEntity chEntity,
                                            List<Programme> allProgrammes) {
        if(allProgrammes == null || allProgrammes.isEmpty()) return;
        // Avoid parallel operations hitting JPA from multiple threads
        allProgrammes.stream()
                .filter(p -> Objects.equals(chDto.id(), p.channel()))
                .forEach(p -> {
                    String t = (p.titles() != null && !p.titles().isEmpty()) ? p.titles().get(0).value() : null;
                    String d = (p.descs() != null && !p.descs().isEmpty()) ? p.descs().get(0).value() : null;
                    var start = EPGTimeParser.parse(p.start());
                    var stop  = EPGTimeParser.parse(p.stop());
                    var existingProg = programmeRepository
                            .findByChannel_IdAndStartTimeAndStopTime(chEntity.getId(), start, stop);
                    if (existingProg.isEmpty() && t != null && !t.isBlank()) {
                        existingProg = programmeRepository
                                .findByChannel_IdAndStartTimeAndStopTimeAndTitle(chEntity.getId(), start, stop, t);
                    }

                    if (existingProg.isPresent()) {
                        var prog = existingProg.get();
                        boolean changed = false;
                        if (!Objects.equals(prog.getTitle(), t)) {
                            prog.setTitle(t);
                            changed = true;
                        }
                        if (!Objects.equals(prog.getDescription(), d)) {
                            prog.setDescription(d);
                            changed = true;
                        }
                        if (changed) programmeRepository.save(prog);
                    } else {
                        programmeRepository.save(ProgrammeEntity.of(chEntity, p));
                    }
                });
    }

    protected List<ChannelEntity> ingestChannel(Tv tv, TvEntity tvEntity) {
        if(tv.channels().isEmpty()) {
            return List.of();
        }
        return tv.channels().stream()
                .map(channel -> {
                    ChannelEntity channelEntity = ChannelEntity.of(tvEntity, channel);
                    ChannelEntity savedChannelEntity = channelRepository.save(channelEntity);
                    savedChannelEntity.setProgrammes(ingestProgram(channel, savedChannelEntity, tv.programmes()));
                    return savedChannelEntity;
                })
                .toList();
    }

    protected List<ProgrammeEntity> ingestProgram(Channel channel, ChannelEntity channelEntity, List<Programme> programmes) {
        if(programmes.isEmpty()) {
            return List.of();
        }
        return programmes.stream()
                .filter(o-> channel.id().equals(o.channel()))
                .map(o-> ProgrammeEntity.of(channelEntity,o))
                .filter(Objects::nonNull)
                .toList();
    }

    public String generate(OffsetDateTime from, OffsetDateTime to) {
        var nowUtc = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        var start = (from == null ? nowUtc : from.withOffsetSameInstant(ZoneOffset.UTC));
        var end   = (to == null ? nowUtc.plusHours(24) : to.withOffsetSameInstant(ZoneOffset.UTC));

        // 1) Channels → DTO
        var rows = channelRepository.findAllChannelsForEpg();
        var channelIds = rows.stream().map(io.github.smling.iptv_mapper.models.dto.epg.ChannelEpgRow::getChannelDbId).toList();
        var names = channelDisplayNameRepository.findByChannel_IdIn(channelIds).stream().collect(java.util.stream.Collectors.groupingBy(e -> e.getChannel().getId()));
        var urls  = channelUrlRepository.findByChannel_IdIn(channelIds).stream().collect(java.util.stream.Collectors.groupingBy(e -> e.getChannel().getId()));

        var channels = rows.stream().map(r -> {
            var dnList = new java.util.ArrayList<io.github.smling.iptv_mapper.models.dto.epg.Text>();
            var byId = names.get(r.getChannelDbId());
            if (byId != null && !byId.isEmpty()) {
                byId.stream().sorted(java.util.Comparator.comparing(e -> e.getPosition() == null ? 0 : e.getPosition()))
                        .forEach(e -> dnList.add(new io.github.smling.iptv_mapper.models.dto.epg.Text(e.getLang(), e.getName())));
            } else {
                String fallback = r.getDisplayName();
                if (fallback == null || fallback.isBlank()) fallback = r.getXmltvId();
                dnList.add(new io.github.smling.iptv_mapper.models.dto.epg.Text(null, fallback));
            }
            var urlList = new java.util.ArrayList<io.github.smling.iptv_mapper.models.dto.epg.UrlRef>();
            var urlById = urls.get(r.getChannelDbId());
            if (urlById != null) {
                urlById.forEach(u -> urlList.add(new io.github.smling.iptv_mapper.models.dto.epg.UrlRef(u.getSystem(), u.getUrl())));
            }
            return new io.github.smling.iptv_mapper.models.dto.epg.Channel(r.getXmltvId(), dnList, java.util.List.of(), urlList);
        }).toList();

        // 2) Programmes → DTO via entity mapping to keep in sync
        var progs = programmeRepository.findEntitiesBetween(start, end).stream()
                .map(io.github.smling.iptv_mapper.models.dto.epg.Programme::fromEntity)
                .toList();

        // 3) Tv root
        var tv = new Tv(
                null, // date
                null, // source-info-name
                null, // source-info-url
                null, // source-data-url
                appName, // generator-info-name
                getGeneratorInfoUrl(), // generator-info-url
                channels,
                progs
        );

        try {
            return XmlMapperFactory.ofEPG().writeValueAsString(tv);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize XMLTV", e);
        }
    }

    /**
     * Generate XMLTV and write to the provided stream.
     * @param from start time (optional)
     * @param to   end time (optional)
     * @param mappedOnly when {@code true}, only channels/programmes that have mappings to M3U items are included.
     *                   When {@code false}, all channels/programmes are exported.
     */
    public void generateTo(java.io.OutputStream out, OffsetDateTime from, OffsetDateTime to, boolean mappedOnly) {
        var nowUtc = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        var start = (from == null ? nowUtc : from.withOffsetSameInstant(ZoneOffset.UTC));
        var end   = (to == null ? nowUtc.plusHours(24) : to.withOffsetSameInstant(ZoneOffset.UTC));

        var rows = mappedOnly
                ? channelRepository.findAllChannelsForEpg()
                : channelRepository.findAllChannelsForEpgAll();

        var channelIds = rows.stream().map(io.github.smling.iptv_mapper.models.dto.epg.ChannelEpgRow::getChannelDbId).toList();
        var names = channelDisplayNameRepository.findByChannel_IdIn(channelIds).stream().collect(java.util.stream.Collectors.groupingBy(e -> e.getChannel().getId()));
        var urls  = channelUrlRepository.findByChannel_IdIn(channelIds).stream().collect(java.util.stream.Collectors.groupingBy(e -> e.getChannel().getId()));

        var channels = rows.stream().map(r -> {
            var dnList = new java.util.ArrayList<io.github.smling.iptv_mapper.models.dto.epg.Text>();
            var byId = names.get(r.getChannelDbId());
            if (byId != null && !byId.isEmpty()) {
                byId.stream().sorted(java.util.Comparator.comparing(e -> e.getPosition() == null ? 0 : e.getPosition()))
                        .forEach(e -> dnList.add(new io.github.smling.iptv_mapper.models.dto.epg.Text(e.getLang(), e.getName())));
            } else {
                String fallback = r.getDisplayName();
                if (fallback == null || fallback.isBlank()) fallback = r.getXmltvId();
                dnList.add(new io.github.smling.iptv_mapper.models.dto.epg.Text(null, fallback));
            }
            var urlList = new java.util.ArrayList<io.github.smling.iptv_mapper.models.dto.epg.UrlRef>();
            var urlById = urls.get(r.getChannelDbId());
            if (urlById != null) {
                urlById.forEach(u -> urlList.add(new io.github.smling.iptv_mapper.models.dto.epg.UrlRef(u.getSystem(), u.getUrl())));
            }
            return new io.github.smling.iptv_mapper.models.dto.epg.Channel(r.getXmltvId(), dnList, java.util.List.of(), urlList);
        }).toList();

        java.util.List<ProgrammeEntity> programmeEntities = programmeRepository.findEntitiesBetween(start, end);
        if (mappedOnly) {
            java.util.Set<java.util.UUID> allowed = new java.util.HashSet<>(channelIds);
            programmeEntities = programmeEntities.stream()
                    .filter(p -> p.getChannel() != null && allowed.contains(p.getChannel().getId()))
                    .toList();
        }

        var progs = programmeEntities.stream()
                .map(io.github.smling.iptv_mapper.models.dto.epg.Programme::fromEntity)
                .toList();

        var tv = new Tv(
                null, // date
                null, // source-info-name
                null, // source-info-url
                null, // source-data-url
                appName, // generator-info-name
                getGeneratorInfoUrl(), // generator-info-url
                channels,
                progs
        );

        try {
            XmlMapperFactory.ofEPG().writeValue(out, tv);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to stream XMLTV", e);
        }
    }

    public void generateTo(java.io.OutputStream out, OffsetDateTime from, OffsetDateTime to) {
        generateTo(out, from, to, true);
    }

    private String getGeneratorInfoUrl() {
        try {
            String host = environment.getProperty("iptv.base-url"); // optional override
            if (host == null || host.isBlank()) {
                String hostname = InetAddress.getLocalHost().getHostName();
                String port = environment.getProperty("server.port", "8080");
                host = "http://" + hostname + ":" + port;
            }
            return host;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
