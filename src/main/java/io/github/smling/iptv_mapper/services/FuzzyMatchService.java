package io.github.smling.iptv_mapper.services;

import io.github.smling.iptv_mapper.FuzzyChannelMatcherSimmetrics;
import io.github.smling.iptv_mapper.models.dao.M3UItemChannelMapEntity;
import io.github.smling.iptv_mapper.models.dao.epg.ChannelEntity;
import io.github.smling.iptv_mapper.models.dao.m3u.M3UItemEntity;
import io.github.smling.iptv_mapper.models.dao.m3u.M3UPlaylistEntity;
import io.github.smling.iptv_mapper.models.dto.m3u.M3UPlaylist;
import io.github.smling.iptv_mapper.repositories.M3UItemChannelMapRepository;
import io.github.smling.iptv_mapper.repositories.epg.ChannelRepository;
import io.github.smling.iptv_mapper.repositories.m3u.M3UPlaylistRepository;
import jakarta.transaction.Transactional;
import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class FuzzyMatchService {
    private final Logger logger = LoggerFactory.getLogger(FuzzyMatchService.class);

    private final M3UPlaylistRepository m3UPlaylistRepository;
    private final ChannelRepository channelRepository;
    private final M3UItemChannelMapRepository repo;
    private final Clock clock;


    public FuzzyMatchService(M3UPlaylistRepository m3UPlaylistRepository,
                             ChannelRepository channelRepository, M3UItemChannelMapRepository repo, Clock clock
    ) {
        this.m3UPlaylistRepository = m3UPlaylistRepository;
        this.channelRepository = channelRepository;
        this.repo = repo;
        this.clock = clock;
    }

    public void match() {
        List<M3UPlaylistEntity> playlists = m3UPlaylistRepository.findAll();
        List<ChannelEntity> channelEntityList = channelRepository.findAll();
        playlists.parallelStream()
                        .forEach(m3UPlaylistEntity -> {
                            m3UPlaylistEntity.getItems().parallelStream()
                                    .forEach(m3UItemEntity -> {
                                        String m3uPlayListTitle = m3UItemEntity.getTitle() + " " + m3UItemEntity.getTvgId();
                                        FuzzyChannelMatcherSimmetrics.match(m3uPlayListTitle, channelEntityList)
                                                .ifPresent(matchResult -> {
                                                    logger.debug("Best match with M3U playlist title {}: {} ({}) score={}", m3uPlayListTitle,
                                                            matchResult.entity().getChannelId(), matchResult.entity().getDisplayName(), matchResult.score());
                                                    upsertAutoMapping(m3UItemEntity, matchResult.entity(), matchResult.score(), "admin");
                                                });
                                    });

                        });
    }

    /** Upsert auto mapping if not manually locked. */
    @Transactional
    public void upsertAutoMapping(M3UItemEntity item,
                                  ChannelEntity matchedChannel,
                                  double score,
                                  String method) {

        var existing = repo.findByItemId(item.getId());
        if (existing.isPresent()) {
            var map = existing.get();
            if (map.isManual()) {
                // locked by human — DO NOT overwrite
                return;
            }
            // update auto mapping
            map.setChannel(matchedChannel);
            map.setUpdatedAt(java.time.OffsetDateTime.now(clock));
            // (optionally update confidence/method)
        } else {
            repo.save(M3UItemChannelMapEntity.ofAuto(item, matchedChannel, score, method, clock));
        }
    }

    /** Manually set mapping (locks it) */
    @Transactional
    public void setManualMapping(M3UItemEntity item, ChannelEntity channel, String note) {
        var existing = repo.findByItemId(item.getId());
        if (existing.isPresent()) {
            var map = existing.get();
            map.setChannel(channel);
            // convert to manual lock:
            // simplest: delete & re-create as manual to ensure timestamps and fields
            repo.delete(map);
        }
        repo.save(M3UItemChannelMapEntity.ofManual(item, channel, note, clock));
    }
}
