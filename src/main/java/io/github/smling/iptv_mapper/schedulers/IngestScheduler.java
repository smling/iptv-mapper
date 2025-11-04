package io.github.smling.iptv_mapper.schedulers;

import io.github.smling.iptv_mapper.services.FuzzyMatchService;
import io.github.smling.iptv_mapper.services.epg.EPGService;
import io.github.smling.iptv_mapper.services.epg.ProgrammeService;
import io.github.smling.iptv_mapper.services.m3u.M3UService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class IngestScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestScheduler.class);

    private final Clock clock;
    private final M3UService m3uService;
    private final EPGService epgService;
    private final FuzzyMatchService fuzzyMatchService;
    private final ProgrammeService programmeService;

    /**
     * Default: every day at 03:00 AM
     * Example cron: 0 0 3 * * *
     */
    @Value("${app.ingest.cron:0 0 3 * * *}")
    private String ingestCron;

    @Value("${app.cleanup.days:7}")
    private int defaultDaysToCleanUpProgrammes;

    @Value("${app.ingest.enabled:true}")
    private boolean ingestEnabled;

    public IngestScheduler(Clock clock, M3UService m3uService, EPGService epgService, FuzzyMatchService fuzzyMatchService, ProgrammeService programmeService) {
        this.clock = clock;
        this.m3uService = m3uService;
        this.epgService = epgService;
        this.fuzzyMatchService = fuzzyMatchService;

        this.programmeService = programmeService;
    }

    /**
     * This method runs automatically every day at 03:00 AM (default),
     * or at whatever time is set via environment variable APP_INGEST_CRON.
     */
    @Scheduled(cron = "${app.ingest.cron:0 0 3 * * *}", zone = "UTC")
    public void autoIngest() {
        if (!ingestEnabled) {
            log.info("🛑 Scheduled ingestion is disabled (app.ingest.enabled=false). Skipping run.");
            return;
        }

        log.info("🕒 Scheduled ingestion triggered (cron: {}).", ingestCron);
        Instant totalStart = Instant.now(clock);
        try {
            // --- M3U ingestion ---
            Instant m3uStart = Instant.now(clock);
            log.info("🚀 Starting M3U ingestion...");
            m3uService.ingestDataSources();
            Duration m3uDuration = Duration.between(m3uStart, Instant.now(clock));
            log.info("✅ M3U ingestion completed in {} ms", m3uDuration.toMillis());

            // --- EPG ingestion ---
            Instant epgStart = Instant.now(clock);
            log.info("📺 Starting EPG ingestion...");
            epgService.ingestDataSources();
            Duration epgDuration = Duration.between(epgStart, Instant.now(clock));
            log.info("✅ EPG ingestion completed in {} ms", epgDuration.toMillis());

            // --- Fuzzy match ---
            Instant matchStart = Instant.now(clock);
            log.info("🧩 Starting fuzzy match...");
            fuzzyMatchService.match();
            Duration matchDuration = Duration.between(matchStart, Instant.now(clock));
            log.info("✅ Fuzzy match completed in {} ms", matchDuration.toMillis());

            // --- Clean up ---
            Instant cleanUpStart = Instant.now(clock);
            log.info("🧩 Starting Clean up...");
            programmeService.cleanup(defaultDaysToCleanUpProgrammes);
            Duration cleanUpDuration = Duration.between(cleanUpStart, Instant.now(clock));
            log.info("✅ Clean up completed in {} ms", matchDuration.toMillis());

            // --- Summary ---
            Duration totalDuration = Duration.between(totalStart, Instant.now(clock));
            log.info("🏁 All ingestion tasks completed successfully in {} ms (≈ {} sec)",
                    totalDuration.toMillis(), totalDuration.toSeconds());

        } catch (Exception e) {
            Duration totalDuration = Duration.between(totalStart, Instant.now(clock));
            log.error("❌ Ingestion failed after {} ms: {}", totalDuration.toMillis(), e.getMessage(), e);
        }
    }
}