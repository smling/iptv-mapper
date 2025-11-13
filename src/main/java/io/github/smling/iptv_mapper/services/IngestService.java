package io.github.smling.iptv_mapper.services;

import io.github.smling.iptv_mapper.models.DataSourceType;
import io.github.smling.iptv_mapper.models.dao.DataSourceEntity;
import io.github.smling.iptv_mapper.repositories.DataSourceRepository;
import org.springframework.context.ApplicationContext;
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
    private final ApplicationContext applicationContext;
    private final Logger logger = LoggerFactory.getLogger(IngestService.class);

    public IngestService(DataSourceRepository dataSourceRepo, ApplicationContext applicationContext) {
        this.dataSourceRepo = dataSourceRepo;
        this.applicationContext = applicationContext;
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

        // Keep parallelism modest to avoid exhausting DB connections
        int threads = Math.min(2, Math.max(1, sources.size()));
        try (ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
            var t = new Thread(r, dataSourceType.name() + "-ingest");
            t.setDaemon(true);
            return t;
        })) {
            var futures = sources.stream()
                    .map(ds -> CompletableFuture.runAsync(() -> {
                        try {
                            // Call through Spring proxy so @Transactional applies
                            IngestService proxy = (IngestService) applicationContext.getBean(this.getClass());
                            proxy.ingestOneInTx(ds);
                        } catch (Exception e) {
                            logger.warn("Ingest failed for {} source [{}] {}: {}", getDataSourcesType(), ds.getId(), ds.getUrl(), e, e);
                        }
                    }, pool))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(futures).join();
        }
    }

    /**
     * Run a single-source ingestion in its own transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public abstract void ingestOneInTx(DataSourceEntity ds);

    public abstract DataSourceType getDataSourcesType();
}
