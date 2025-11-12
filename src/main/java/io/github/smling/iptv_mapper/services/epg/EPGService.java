package io.github.smling.iptv_mapper.services.epg;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.smling.iptv_mapper.EPGClient;
import io.github.smling.iptv_mapper.StringUtil;
import io.github.smling.iptv_mapper.UriChecker;
import io.github.smling.iptv_mapper.factories.XmlMapperFactory;
import io.github.smling.iptv_mapper.models.DataSourceType;
import io.github.smling.iptv_mapper.models.dao.DataSourceEntity;
import io.github.smling.iptv_mapper.models.dao.epg.ChannelEntity;
import io.github.smling.iptv_mapper.models.dao.epg.ProgrammeEntity;
import io.github.smling.iptv_mapper.models.dao.epg.TvEntity;
import io.github.smling.iptv_mapper.models.dto.epg.Channel;
import io.github.smling.iptv_mapper.models.dto.epg.Programme;
import io.github.smling.iptv_mapper.models.dto.epg.Tv;
import io.github.smling.iptv_mapper.parsers.EPGTimeParser;
import io.github.smling.iptv_mapper.repositories.DataSourceRepository;
import io.github.smling.iptv_mapper.repositories.epg.ChannelRepository;
import io.github.smling.iptv_mapper.repositories.epg.ProgrammeRepository;
import io.github.smling.iptv_mapper.repositories.epg.TvRepository;
import io.github.smling.iptv_mapper.services.IngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

@Service
public class EPGService extends IngestService {
    private static final Logger logger = LoggerFactory.getLogger(EPGService.class);
    private final Clock clock;
    private final TvRepository tvRepository;
    private final ChannelRepository channelRepository;
    private final ProgrammeRepository programmeRepository;
    private final String appName;
    private final Environment environment;

    public EPGService(DataSourceRepository dataSourceRepo,
                      Clock clock,
                      TvRepository tvRepository,
                      ChannelRepository channelRepository,
                      ProgrammeRepository programmeRepository,
                      @Value("${spring.application.name}") String appName, Environment environment
    ) {
        super(dataSourceRepo);
        this.clock = clock;
        this.tvRepository = tvRepository;
        this.channelRepository = channelRepository;
        this.programmeRepository = programmeRepository;
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
        if(!new UriChecker().isUrlReachable(requestUri)) {
            logger.debug("⏭️ Skip data source {} (type={}, enabled={})", ds.getId(), ds.getType(), ds.isEnabled());
            return; // do not attempt fetch if not reachable
        }

        logger.info("📡 Starting EPG ingest for {}", requestUri);
        try {
            // Download first to avoid premature EOF when parsing is slow due to DB writes
            java.nio.file.Path tmp = new EPGClient().downloadToTempFile(requestUri);
            logger.debug("💾 EPG downloaded to temp file: {}", tmp);
            try (InputStream in = java.nio.file.Files.newInputStream(tmp)) {
                streamIngest(in, ds);
                logger.info("✅ Completed EPG ingest for {}", requestUri);
            } finally {
                try { java.nio.file.Files.deleteIfExists(tmp); } catch (Exception ignore) {}
            }
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("🛑 Interrupted while fetching {}", requestUri, e);
        } catch(IOException e) {
            logger.error("❌ I/O error occurred while fetching {}", requestUri, e);
        }
    }

    private void streamIngest(InputStream in, DataSourceEntity ds) {
        XMLInputFactory f = XMLInputFactory.newFactory();
        XMLStreamReader r = null;
        try {
            r = f.createXMLStreamReader(in);

            TvEntity tvEntity = null;
            String tvGenName = null;
            String tvGenUrl = null;
            Map<String, ChannelEntity> channelCache = new HashMap<>();
            int chCreated = 0, chUpdated = 0, progCreated = 0, progUpdated = 0;

            while (r.hasNext()) {
                int ev = r.next();
                if (ev != XMLStreamConstants.START_ELEMENT) continue;
                String name = r.getLocalName();

                if ("tv".equals(name)) {
                    tvGenName = attr(r, "generator-info-name");
                    tvGenUrl  = attr(r, "generator-info-url");
                    Optional<TvEntity> existing = tvRepository
                            .findByGeneratorInfoNameAndGeneratorInfoUrl(tvGenName, tvGenUrl);
                    if (existing.isPresent()) {
                        tvEntity = existing.get();
                        logger.debug("🔁 Reusing TvEntity id={} gen='{}' url='{}'", tvEntity.getId(), tvGenName, tvGenUrl);
                    } else {
                        tvEntity = tvRepository.save(TvEntity.of(ds, new Tv(tvGenName, tvGenUrl, List.of(), List.of()), clock));
                        logger.debug("🆕 Created TvEntity gen='{}' url='{}'", tvGenName, tvGenUrl);
                    }
                } else if ("channel".equals(name)) {
                    if (tvEntity == null) continue; // safeguard
                    String chId = attr(r, "id");
                    String displayName = null;
                    // descend until end of channel
                    while (r.hasNext()) {
                        int inner = r.next();
                        if (inner == XMLStreamConstants.START_ELEMENT && "display-name".equals(r.getLocalName())) {
                            displayName = text(r);
                        } else if (inner == XMLStreamConstants.END_ELEMENT && "channel".equals(r.getLocalName())) {
                            break;
                        }
                    }
                    Optional<ChannelEntity> chOpt = channelRepository.findByTv_IdAndChannelId(tvEntity.getId(), chId)
                            .or(() -> channelRepository.findByChannelId(chId));
                    ChannelEntity chEntity;
                    if (chOpt.isPresent()) {
                        chEntity = chOpt.get();
                    } else {
                        chEntity = channelRepository.save(ChannelEntity.of(tvEntity, new Channel(chId, displayName)));
                        chCreated++;
                        logger.debug("➕ Inserted channel '{}' ('{}')", chId, displayName);
                    }
                    if (!Objects.equals(chEntity.getDisplayName(), displayName) && displayName != null) {
                        chEntity.setDisplayName(displayName);
                        channelRepository.save(chEntity);
                        chUpdated++;
                        logger.debug("✏️ Updated channel '{}' displayName -> '{}'", chId, displayName);
                    }
                    channelCache.put(chId, chEntity);
                } else if ("programme".equals(name)) {
                    String start = attr(r, "start");
                    String stop  = attr(r, "stop");
                    String chRef = attr(r, "channel");
                    String title = null;
                    String desc  = null;

                    while (r.hasNext()) {
                        int inner = r.next();
                        if (inner == XMLStreamConstants.START_ELEMENT) {
                            String ln = r.getLocalName();
                            if ("title".equals(ln)) { title = text(r); }
                            else if ("desc".equals(ln)) { desc = text(r); }
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
                    var existingProg = programmeRepository
                            .findByChannel_IdAndStartTimeAndStopTime(chEntity.getId(), startTs, stopTs);

                    if (existingProg.isPresent()) {
                        var prog = existingProg.get();
                        boolean changed = false;
                        if (!Objects.equals(prog.getTitle(), title)) { prog.setTitle(title); changed = true; }
                        if (!Objects.equals(prog.getDescription(), desc)) { prog.setDescription(desc); changed = true; }
                        if (changed) { programmeRepository.save(prog); progUpdated++; }
                    } else {
                        programmeRepository.save(ProgrammeEntity.of(chEntity,
                                new Programme(start, stop, chRef, title, desc)));
                        progCreated++;
                    }
                }
            }
            logger.info("📦 EPG ingest summary: channels created={}, updated={}, programmes created={}, updated={}",
                    chCreated, chUpdated, progCreated, progUpdated);
        } catch (XMLStreamException e) {
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
                if (!Objects.equals(chEntity.getDisplayName(), chDto.displayName())) {
                    chEntity.setDisplayName(chDto.displayName());
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
                                            java.util.List<Programme> allProgrammes) {
        if(allProgrammes == null || allProgrammes.isEmpty()) return;
        // Avoid parallel operations hitting JPA from multiple threads
        allProgrammes.stream()
                .filter(p -> Objects.equals(chDto.id(), p.channel()))
                .forEach(p -> {
                    var start = EPGTimeParser.parse(p.start());
                    var stop  = EPGTimeParser.parse(p.stop());
                    var existingProg = programmeRepository
                            .findByChannel_IdAndStartTimeAndStopTime(chEntity.getId(), start, stop);

                    if (existingProg.isPresent()) {
                        var prog = existingProg.get();
                        boolean changed = false;
                        if (!Objects.equals(prog.getTitle(), p.title())) {
                            prog.setTitle(p.title());
                            changed = true;
                        }
                        if (!Objects.equals(prog.getDescription(), p.desc())) {
                            prog.setDescription(p.desc());
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
        var channels = channelRepository.findAllChannelsWithMappingId().stream()
                .map(c -> new Channel(c.getXmltvId(), c.getDisplayName()))
                .toList();

        // 2) Programmes → DTO (format timestamps as "yyyyMMddHHmmss +0000")
        var progs = programmeRepository.findProgrammesBetween(start, end).stream()
                .map(p -> new Programme(
                        EPGTimeParser.toIsoInstantString(p.getStartUtc()),
                        EPGTimeParser.toIsoInstantString(p.getStopUtc()),
                        p.getChannelXmltvId(),
                        StringUtil.nullSafe(p.getTitle()),
                        StringUtil.nullSafe(p.getDesc())
                ))
                .toList();

        // 3) Tv root
        var tv = new Tv(
                appName,                       // generator-info-name
                getGeneratorInfoUrl(),               // generator-info-url
                channels,
                progs
        );

        try {
            return XmlMapperFactory.ofEPG().writeValueAsString(tv);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize XMLTV", e);
        }
    }

    public void generateTo(java.io.OutputStream out, OffsetDateTime from, OffsetDateTime to) {
        var nowUtc = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        var start = (from == null ? nowUtc : from.withOffsetSameInstant(ZoneOffset.UTC));
        var end   = (to == null ? nowUtc.plusHours(24) : to.withOffsetSameInstant(ZoneOffset.UTC));

        var channels = channelRepository.findAllChannelsWithMappingId().stream()
                .map(c -> new Channel(c.getXmltvId(), c.getDisplayName()))
                .toList();

        var progs = programmeRepository.findProgrammesBetween(start, end).stream()
                .map(p -> new Programme(
                        EPGTimeParser.toIsoInstantString(p.getStartUtc()),
                        EPGTimeParser.toIsoInstantString(p.getStopUtc()),
                        p.getChannelXmltvId(),
                        StringUtil.nullSafe(p.getTitle()),
                        StringUtil.nullSafe(p.getDesc())
                ))
                .toList();

        var tv = new Tv(
                appName,
                getGeneratorInfoUrl(),
                channels,
                progs
        );

        try {
            XmlMapperFactory.ofEPG().writeValue(out, tv);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to stream XMLTV", e);
        }
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
