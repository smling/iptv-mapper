package io.github.smling.iptv_mapper.services;

import io.github.smling.iptv_mapper.models.DataSourceType;
import io.github.smling.iptv_mapper.models.dao.DataSourceEntity;
import io.github.smling.iptv_mapper.repositories.DataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class IngestService {
    private final DataSourceRepository dataSourceRepo;
    private final Logger logger = LoggerFactory.getLogger(IngestService.class);

    public IngestService(DataSourceRepository dataSourceRepo) {
        this.dataSourceRepo = dataSourceRepo;
    }

    /**
     * Fetch all ENABLED data sources from DB and ingest them in parallel.
     * Each source runs in its own transaction; one failure won't stop others.
     */
    public void ingestDataSources() {
        DataSourceType dataSourceType = getDataSourcesType();
        List<DataSourceEntity> sources = dataSourceRepo.findByTypeOrderByPriorityAsc(dataSourceType)
                .stream()
                .filter(DataSourceEntity::isEnabled)
                .toList();

        if (sources.isEmpty()) {
            logger.info("No enabled {} sources found.", dataSourceType);
            return;
        }

        int threads = Math.min(32, Math.max(2, Runtime.getRuntime().availableProcessors() * 2));
        try (ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
            var t = new Thread(r, dataSourceType.name() + "-ingest");
            t.setDaemon(true);
            return t;
        })) {
            var futures = sources.stream()
                    .map(ds -> CompletableFuture.runAsync(() -> safeIngestOne(ds), pool))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(futures).join();
        }
    }

    /**
     * Catch/Log per-source errors so the parallel batch continues.
     */
    private void safeIngestOne(DataSourceEntity ds) {
        try {
            ingestOneInTx(ds);
        } catch (Exception e) {
            logger.warn("Ingest failed for {} source [{}] {}: {}", getDataSourcesType(), ds.getId(), ds.getUrl(), e, e);
        }
    }

    /**
     * Run a single-source ingestion in its own transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public abstract void ingestOneInTx(DataSourceEntity ds);

    public abstract DataSourceType getDataSourcesType();
}
