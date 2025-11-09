package io.github.smling.iptv_mapper.services.epg;

import io.github.smling.iptv_mapper.models.dao.epg.EpisodeNumberEntity;
import io.github.smling.iptv_mapper.repositories.epg.EpisodeNumberRepository;
import io.github.smling.iptv_mapper.services.CRUDService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class EpisodeNumberService implements CRUDService<EpisodeNumberEntity> {
    private final EpisodeNumberRepository repo;
    public EpisodeNumberService(EpisodeNumberRepository repo) { this.repo = repo; }

    @Override
    public EpisodeNumberEntity create(EpisodeNumberEntity e) {
        return repo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public EpisodeNumberEntity get(UUID id) {
        return repo
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("episode_number " + id + " not found"))
                ;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EpisodeNumberEntity> list(Pageable pageable) { return repo.findAll(pageable); }

    @Override
    public EpisodeNumberEntity update(UUID id, EpisodeNumberEntity u) {
        var e = get(id);
        return repo.save(new EpisodeNumberEntity(
                e.getId(),
                u.getProgramme() != null ? u.getProgramme() : e.getProgramme(),
                u.getSystem() != null ? u.getSystem() : e.getSystem(),
                u.getValue() != null ? u.getValue() : e.getValue()
        ));
    }

    @Override
    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new EntityNotFoundException("episode_number " + id + " not found");
        repo.deleteById(id);
    }
}
