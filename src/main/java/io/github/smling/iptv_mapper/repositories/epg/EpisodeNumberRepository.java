package io.github.smling.iptv_mapper.repositories.epg;

import io.github.smling.iptv_mapper.models.dao.epg.EpisodeNumberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EpisodeNumberRepository extends JpaRepository<EpisodeNumberEntity, UUID> {
    List<EpisodeNumberEntity> findByProgramme_Id(UUID programmeId);

    long deleteByProgramme_Id(UUID programmeId);
}
