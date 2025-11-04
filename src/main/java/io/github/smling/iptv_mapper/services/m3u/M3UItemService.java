package io.github.smling.iptv_mapper.services.m3u;

import io.github.smling.iptv_mapper.models.dao.m3u.M3UItemEntity;
import io.github.smling.iptv_mapper.repositories.m3u.M3UItemRepository;
import io.github.smling.iptv_mapper.services.CRUDService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class M3UItemService implements CRUDService<M3UItemEntity> {
    private final M3UItemRepository repo;

    public M3UItemService(M3UItemRepository repo) { this.repo = repo; }

    @Override
    public M3UItemEntity create(M3UItemEntity toCreate) { return repo.save(toCreate); }

    @Override
    @Transactional(readOnly = true)
    public M3UItemEntity get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("m3u_item " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<M3UItemEntity> list(Pageable pageable) { return repo.findAll(pageable); }

    @Override
    public M3UItemEntity update(UUID id, M3UItemEntity toUpdate) {
        var e = get(id);
        return repo.save(toUpdate);
    }

    @Override
    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new EntityNotFoundException("m3u_item " + id + " not found");
        repo.deleteById(id);
    }
}