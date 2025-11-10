package io.github.smling.iptv_mapper.services;

import io.github.smling.iptv_mapper.FuzzyChannelMatcherSimmetrics;
import io.github.smling.iptv_mapper.models.dao.M3UItemChannelMapEntity;
import io.github.smling.iptv_mapper.models.dao.epg.ChannelEntity;
import io.github.smling.iptv_mapper.models.dao.m3u.M3UItemEntity;
import io.github.smling.iptv_mapper.repositories.M3UItemChannelMapRepository;
import io.github.smling.iptv_mapper.repositories.epg.ChannelRepository;
import io.github.smling.iptv_mapper.repositories.m3u.M3UItemRepository;
 
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

@Service
public class FuzzyMatchService {
    private final Logger logger = LoggerFactory.getLogger(FuzzyMatchService.class);

    private final ChannelRepository channelRepository;
    private final M3UItemChannelMapRepository repo;
    private final M3UItemRepository itemRepository;
    private final Clock clock;


    public FuzzyMatchService(ChannelRepository channelRepository,
                             M3UItemChannelMapRepository repo,
                             M3UItemRepository itemRepository,
                             Clock clock
    ) {
        this.channelRepository = channelRepository;
        this.repo = repo;
        this.itemRepository = itemRepository;
        this.clock = clock;
    }

    public void match() {
        // Fetch once to minimize DB round-trips
        List<ChannelEntity> channels = channelRepository.findAll();
        List<M3UItemEntity> unmappedItems = itemRepository.findAllUnmapped();

        if (unmappedItems.isEmpty() || channels.isEmpty()) {
            logger.info("No unmapped items or no channels available to match.");
            return;
        }

        // Build mappings for unmapped items only
        unmappedItems.parallelStream().forEach(item -> {
            String key = String.format("%s %s",
                    item.getTitle() == null ? "" : item.getTitle(),
                    item.getTvgId() == null ? "" : item.getTvgId()).trim();

            FuzzyChannelMatcherSimmetrics.match(key, channels).ifPresent(best -> {
                logger.debug("Best match for '{}' => {} ({}) score={}",
                        key, best.entity().getChannelId(), best.entity().getDisplayName(), best.score());
                // Directly create mapping; item is guaranteed unmapped in this batch
                repo.save(M3UItemChannelMapEntity.ofAuto(item, best.entity(), best.score(), "auto", clock));
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
