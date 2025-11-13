package io.github.smling.iptv_mapper.repositories.epg;

import io.github.smling.iptv_mapper.models.dao.epg.ChannelDisplayNameEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChannelDisplayNameRepository extends JpaRepository<ChannelDisplayNameEntity, UUID> {
    List<ChannelDisplayNameEntity> findByChannel_IdIn(Collection<UUID> channelIds);
}

