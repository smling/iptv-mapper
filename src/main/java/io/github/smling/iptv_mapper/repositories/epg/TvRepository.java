package io.github.smling.iptv_mapper.repositories.epg;

import io.github.smling.iptv_mapper.models.dao.epg.TvEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TvRepository extends JpaRepository<TvEntity, UUID> {
    List<TvEntity> findByDataSource_Id(UUID dataSourceId);

    boolean existsByDataSource_Id(UUID dataSourceId);
}

