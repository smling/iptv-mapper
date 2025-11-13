package io.github.smling.iptv_mapper.repositories.epg;

import io.github.smling.iptv_mapper.models.dao.epg.ChannelUrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChannelUrlRepository extends JpaRepository<ChannelUrlEntity, UUID> {
    List<ChannelUrlEntity> findByChannel_IdIn(Collection<UUID> channelIds);
}

