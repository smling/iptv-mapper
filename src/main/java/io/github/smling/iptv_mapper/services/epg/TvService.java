package io.github.smling.iptv_mapper.services.epg;

import io.github.smling.iptv_mapper.models.dao.epg.TvEntity;
import io.github.smling.iptv_mapper.repositories.epg.TvRepository;
import io.github.smling.iptv_mapper.services.CRUDService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional
public class TvService implements CRUDService<TvEntity> {
    private final TvRepository repo;

    public TvService(TvRepository repo) { this.repo = repo; }

    @Override
    public TvEntity create(TvEntity toCreate) {
        var now = OffsetDateTime.now();
        return repo.save(toCreate);
    }

    @Override @Transactional(readOnly = true)
    public TvEntity get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("tv " + id + " not found"));
    }

    @Override @Transactional(readOnly = true)
    public Page<TvEntity> list(Pageable pageable) { return repo.findAll(pageable); }

    @Override
    public TvEntity update(UUID id, TvEntity toUpdate) {
        var e = get(id);
        return repo.save(toUpdate);
    }

    @Override
    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new EntityNotFoundException("tv " + id + " not found");
        repo.deleteById(id);
    }
}