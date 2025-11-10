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
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
            logger.debug("Skip data source {} (type={}, enabled={})", ds.getId(), ds.getType(), ds.isEnabled());
        }

        try {
            Tv tv = new EPGClient().fetchEpg(requestUri);
            logger.info("EPG source [{}] fetched {} channels.", ds.getUrl(), tv.channels().size());
            Optional<TvEntity> existing = tvRepository
                    .findByGeneratorInfoNameAndGeneratorInfoUrl(tv.generatorInfoName(), tv.generatorInfoUrl());

            TvEntity tvEntity;
            if (existing.isPresent()) {
                tvEntity = existing.get();
                logger.debug("Reusing existing TvEntity id={} (generator='{}', url='{}')",
                        tvEntity.getId(), tv.generatorInfoName(), tv.generatorInfoUrl());
            } else {
                logger.debug("Creating new TvEntity (generator='{}', url='{}')",
                        tv.generatorInfoName(), tv.generatorInfoUrl());
                tvEntity = tvRepository.save(TvEntity.of(ds, tv, clock));
            }

            // Upsert channels and programmes for this TV
            upsertChannelsAndProgrammes(tv, tvEntity);

        } catch(InterruptedException | IOException e) {
            logger.error("Error occurred on fetching {}", requestUri);
        }
    }

    private void upsertChannelsAndProgrammes(Tv tv, TvEntity tvEntity) {
        if(tv.channels().isEmpty()) return;
        var channels = tv.channels();
        channels.parallelStream().forEach(chDto -> {
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
        allProgrammes.parallelStream()
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
        return tv.channels().parallelStream()
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
        return programmes.parallelStream()
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
        var channels = channelRepository.findAllChannelsWithMappingId().parallelStream()
                .map(c -> new Channel(c.getXmltvId(), c.getDisplayName()))
                .toList();

        // 2) Programmes → DTO (format timestamps as "yyyyMMddHHmmss +0000")
        var progs = programmeRepository.findProgrammesBetween(start, end).parallelStream()
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
