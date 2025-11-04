package io.github.smling.iptv_mapper.services;



import io.github.smling.iptv_mapper.models.dao.DataSourceEntity;
import io.github.smling.iptv_mapper.repositories.DataSourceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
public class DataSourceService implements CRUDService<DataSourceEntity> {

    private final DataSourceRepository repo;
    private final Clock clock;

    public DataSourceService(DataSourceRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    public DataSourceEntity create(DataSourceEntity c) {
        return repo.save(c);
    }

    @Override
    public DataSourceEntity get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("data_source " + id + " not found"));
    }

    @Override
    public Page<DataSourceEntity> list(Pageable pageable) {
        return repo.findAll(pageable);
    }

    public DataSourceEntity update(UUID id, DataSourceEntity u) {
//        var existing = get(id);
//        var replaced = DataSourceEntity.replace(
//                existing,
//                u.type(), u.url(), u.label(), u.countryCode(),
//                u.enabled(), u.priority(), u.notes(),
//                u.lastHttpStatus(), u.lastFetchedAt(), u.lastEtag(), u.lastModifiedHdr(), u.contentChecksum(),
//                clock
//        );
        return repo.save(u);
    }

    @Override
    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new EntityNotFoundException("episode_number " + id + " not found");
        repo.deleteById(id);
    }
}
