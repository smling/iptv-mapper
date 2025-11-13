package io.github.smling.iptv_mapper.services.m3u;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.smling.iptv_mapper.M3UClient;
import io.github.smling.iptv_mapper.UriChecker;
import io.github.smling.iptv_mapper.Reachability;
import io.github.smling.iptv_mapper.models.DataSourceType;
import io.github.smling.iptv_mapper.models.dao.DataSourceEntity;
import io.github.smling.iptv_mapper.models.dao.m3u.M3UItemEntity;
import io.github.smling.iptv_mapper.models.dao.m3u.M3UPlaylistEntity;
import io.github.smling.iptv_mapper.models.dto.m3u.M3UItem;
import io.github.smling.iptv_mapper.models.dto.m3u.M3UItemChannelView;
import io.github.smling.iptv_mapper.models.dto.m3u.M3UPlaylist;
import io.github.smling.iptv_mapper.models.dto.m3u.M3UPlaylistLineView;
import io.github.smling.iptv_mapper.repositories.DataSourceRepository;
import io.github.smling.iptv_mapper.repositories.M3UItemChannelMapRepository;
import io.github.smling.iptv_mapper.repositories.epg.ChannelRepository;
import io.github.smling.iptv_mapper.repositories.m3u.M3UItemRepository;
import io.github.smling.iptv_mapper.repositories.m3u.M3UPlaylistRepository;
import io.github.smling.iptv_mapper.services.IngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class M3UService extends IngestService {
    private final DataSourceRepository dataSourceRepo;
    private final M3UPlaylistRepository playlistRepo;
    private final M3UItemRepository itemRepo;
    private final Clock clock;
    private final Logger logger = LoggerFactory.getLogger(M3UService.class);
    private final M3UItemChannelMapRepository m3UItemChannelMapRepository;

    public M3UService(
            DataSourceRepository dataSourceRepo,
            M3UPlaylistRepository playlistRepo,
            M3UItemRepository itemRepo, ChannelRepository channelRepository,
            Clock clock, M3UItemChannelMapRepository m3UItemChannelMapRepository,
            org.springframework.context.ApplicationContext applicationContext
    ) {
        super(dataSourceRepo, applicationContext);
        this.dataSourceRepo = dataSourceRepo;
        this.playlistRepo = playlistRepo;
        this.itemRepo = itemRepo;
        this.clock = clock;
        this.m3UItemChannelMapRepository = m3UItemChannelMapRepository;
    }

    @Override
    public DataSourceType getDataSourcesType() {
        return DataSourceType.M3U;
    }
    /**
     * Run a single-source ingestion in its own transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ingestOneInTx(DataSourceEntity ds) {
        if (ds.getType() != DataSourceType.M3U || !ds.isEnabled()) {
            logger.debug("Skip data source {} (type={}, enabled={})", ds.getId(), ds.getType(), ds.isEnabled());
            return;
        }

        // Use a per-source client bound to the source URL
        M3UClient client = new M3UClient(URI.create(ds.getUrl()));

        try {
            // Fetch + validate items
            M3UPlaylist playlist = client.get();
            ObjectMapper objectMapper = new ObjectMapper();

            // Persist root playlist first
            OffsetDateTime now = OffsetDateTime.now(clock);

            String json = objectMapper.writeValueAsString(playlist.globalAttributes());
            M3UPlaylistEntity playlistEntity = playlistRepo.findByGlobalAttributes(json)
                    .orElse(M3UPlaylistEntity.of(ds, playlist));
            playlistEntity.setUpdatedAt(now);
            M3UPlaylistEntity savedPaylistEntity = playlistRepo.save(playlistEntity);

            var checker = new UriChecker();
            AtomicInteger updated = new AtomicInteger(0);
            AtomicInteger created = new AtomicInteger(0);
            for (M3UItem item : playlist.items()) {
                var check = checker.check(item.url());
                Reachability reach = check.status();
                String urlStr = item.url() != null ? item.url().toString() : null;
                itemRepo.findByPlaylistIdAndUrl(savedPaylistEntity.getId(), urlStr)
                        .ifPresentOrElse(existing -> {
                            existing.setUrl(urlStr);
                            existing.setUpdatedAt(now);
                            existing.setUrlCheckerResult(reach.name());
                            existing.setUrlCheckerMs(check.responseTimeMillis());
                            itemRepo.save(existing);
                            logger.debug("M3U item exists; updated url/checker: {} -> {} ({} ms)", urlStr, reach, check.responseTimeMillis());
                            updated.incrementAndGet();
                        }, () -> {
                            if (reach == Reachability.Reachable) {
                                M3UItemEntity entity = M3UItemEntity.of(savedPaylistEntity, item);
                                entity.setUrlCheckerResult(reach.name());
                                entity.setUrlCheckerMs(check.responseTimeMillis());
                                itemRepo.save(entity);
                                logger.debug("M3U item new; created with checker result: {} -> {} ({} ms)", urlStr, reach, check.responseTimeMillis());
                                created.incrementAndGet();
                            } else {
                                logger.debug("M3U item unreachable/inaccessible; skipping create: {} -> {}", urlStr, reach);
                            }
                        });
            }

            logger.info("M3U source [{}] processed {} items (updated={}, created={}).",
                    ds.getUrl(), playlist.items().size(), updated.get(), created.get());

            // Optionally: update DS lastFetchedAt, lastHttpStatus, checksum, etc.
            ds.setLastHttpStatus(200)
              .setLastFetchedAt(now)
              .setLastEtag(ds.getLastEtag())
              .setLastModifiedHdr(ds.getLastModifiedHdr())
              .setContentChecksum(ds.getContentChecksum());
            dataSourceRepo.save(ds);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt(); // safe no-op if not interrupted
            // Mark failure on data source
            var failNow = OffsetDateTime.now(clock);

            // Optionally: update DS lastFetchedAt, lastHttpStatus, checksum, etc.
            ds.setLastHttpStatus(599)
              .setLastFetchedAt(failNow)
              .setLastEtag(ds.getLastEtag())
              .setLastModifiedHdr(ds.getLastModifiedHdr())
              .setContentChecksum(ds.getContentChecksum());
            dataSourceRepo.save(ds);
            throw new IllegalStateException("Failed to fetch M3U for " + ds.getUrl(), e);
        }
    }

    public List<M3UItem> getValidItems(List<M3UItem> items) {
        if (items == null || items.isEmpty()) return List.of();
        return items.parallelStream() // parallel execution
                .map(item -> new UriChecker().check(item.url()).isReachable() ? item : null)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Build a full M3U with all mapped items. */
    public String generateAll() {
        List<M3UPlaylistLineView> rows = itemRepo.findPlaylistLinesAll();

        StringBuilder sb = new StringBuilder(16_384);
        sb.append("#EXTM3U\n"); // no url-tvg

        int seq = 1000; // starting channel number (customize if needed)
        for (M3UPlaylistLineView r : rows) {
            String tvgChNo = r.getTvgId() != null && !r.getTvgId().isBlank()
                    ? r.getTvgId()
                    : String.valueOf(seq);

            sb.append("#EXTINF:0 ");
            sb.append("channelID=\"x-ID.").append(seq - 1000).append("\" ");
            sb.append("tvg-chno=\"").append(escape(r.getTvgChno())).append("\" ");
            sb.append("tvg-name=\"").append(escape(ns(r.getTvgName()))).append("\" ");
            sb.append("tvg-id=\"").append(escape(ns(r.getTvgId()))).append("\" ");
            if (r.getTvgLogo() != null && !r.getTvgLogo().isBlank()) {
                sb.append("tvg-logo=\"").append(escape(r.getTvgLogo())).append("\" ");
            }
            sb.append("group-title=\"").append("").append("\","); // no groups in this variant
            sb.append(ns(r.getChannelName())).append("\n");

            sb.append(ns(r.getStreamUrl())).append("\n");
            seq++;
        }
        return sb.toString();
    }

    public Page<M3UItemChannelView> list(Pageable pageable) {
        return m3UItemChannelMapRepository.findAllProjected(pageable);
    }

    private static String ns(String s) { return s == null ? "" : s; }
    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
