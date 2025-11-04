package io.github.smling.iptv_mapper.services.epg;

import io.github.smling.iptv_mapper.models.dao.epg.ProgrammeEntity;
import io.github.smling.iptv_mapper.repositories.epg.ProgrammeRepository;
import io.github.smling.iptv_mapper.services.CRUDService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional
public class ProgrammeService implements CRUDService<ProgrammeEntity> {
    private final Clock clock;
    private final ProgrammeRepository programmeRepository;

    private final Logger logger = LoggerFactory.getLogger(ProgrammeService.class);

    public ProgrammeService(Clock clock, ProgrammeRepository programmeRepository) {
        this.clock = clock;
        this.programmeRepository = programmeRepository; }

    @Override public ProgrammeEntity create(ProgrammeEntity p) { return programmeRepository.save(p); }

    @Value("${app.cleanup.days:7}")
    private int defaultDays;

    @Override @Transactional(readOnly = true)
    public ProgrammeEntity get(UUID id) {
        return programmeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("programme " + id + " not found"));
    }

    @Override @Transactional(readOnly = true)
    public Page<ProgrammeEntity> list(Pageable pageable) { return programmeRepository.findAll(pageable); }

    @Override
    public ProgrammeEntity update(UUID id, ProgrammeEntity u) {
        var e = get(id);
        return programmeRepository.save(u);
    }

    @Override public void delete(UUID id) {
        if (!programmeRepository.existsById(id)) throw new EntityNotFoundException("programme " + id + " not found");
        programmeRepository.deleteById(id);
    }

    @Transactional
    public int cleanup(Integer daysOverride) {
        int days = (daysOverride != null ? daysOverride : defaultDays);
        if (days < 0) {
            throw new IllegalArgumentException("days must be >= 0");
        }

        OffsetDateTime cutoff = OffsetDateTime.now(clock).minusDays(days);
        long start = System.nanoTime();
        int deleted = programmeRepository.deleteOlderThan(cutoff);
        long ms = (System.nanoTime() - start) / 1_000_000;

        logger.info("🧹 Programme cleanup: days={}, cutoff={}, deleted={}, took={}ms",
                days, cutoff, deleted, ms);

        return deleted;
    }
}
