package io.github.smling.iptv_mapper.services.epg;

import io.github.smling.iptv_mapper.models.dao.epg.ChannelEntity;
import io.github.smling.iptv_mapper.models.dto.epg.ChannelProgrammeView;
import io.github.smling.iptv_mapper.repositories.epg.ChannelRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ChannelService  {

    private final ChannelRepository repo;

    public ChannelService(ChannelRepository repo) { this.repo = repo; }

    public ChannelEntity create(ChannelEntity c) { return repo.save(c); }

    @Transactional(readOnly = true)
    public ChannelEntity get(String id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("channel " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public Page<ChannelEntity> list(Pageable pageable) { return repo.findAll(pageable); }

    public ChannelEntity update(ChannelEntity toUpdate) {
        return repo.save(toUpdate);
    }

    public void delete(String id) {
        if (!repo.existsById(id)) throw new EntityNotFoundException("channel " + id + " not found");
        repo.deleteById(id);
    }

    public Page<ChannelProgrammeView> listByChannelDbId(UUID channelDbId, Pageable pageable) {
        return repo.findByChannelDbId(channelDbId, pageable);
    }
}