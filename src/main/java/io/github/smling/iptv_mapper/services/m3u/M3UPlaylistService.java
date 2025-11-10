package io.github.smling.iptv_mapper.services.m3u;

import io.github.smling.iptv_mapper.models.dao.m3u.M3UPlaylistEntity;
import io.github.smling.iptv_mapper.repositories.m3u.M3UPlaylistRepository;
import io.github.smling.iptv_mapper.services.CRUDService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional
public class M3UPlaylistService implements CRUDService<M3UPlaylistEntity> {
    private final M3UPlaylistRepository repo;
    private final Clock clock;

    public M3UPlaylistService(M3UPlaylistRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Override
    public M3UPlaylistEntity create(M3UPlaylistEntity toCreate) {
        var now = OffsetDateTime.now(clock);
        toCreate.setCreatedAt(now);
        toCreate.setUpdatedAt(now);
        return repo.save(toCreate);
    }

    @Override @Transactional(readOnly = true)
    public M3UPlaylistEntity get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("m3u_playlist " + id + " not found"));
    }

    @Override @Transactional(readOnly = true)
    public Page<M3UPlaylistEntity> list(Pageable pageable) { return repo.findAll(pageable); }

    @Override
    public M3UPlaylistEntity update(UUID id, M3UPlaylistEntity toUpdate) {
        toUpdate.setUpdatedAt(OffsetDateTime.now(clock));
        return repo.save(toUpdate);
    }

    @Override
    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new EntityNotFoundException("m3u_playlist " + id + " not found");
        repo.deleteById(id);
    }
}
