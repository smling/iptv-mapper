package io.github.smling.iptv_mapper.repositories.epg;

import io.github.smling.iptv_mapper.models.dao.epg.TvEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TvRepository extends JpaRepository<TvEntity, UUID> {
    Optional<TvEntity> findByDataSource_Id(UUID dataSourceId);

    boolean existsByDataSource_Id(UUID dataSourceId);

    Optional<TvEntity> findByGeneratorInfoNameAndGeneratorInfoUrl(String generatorInfoName, String generatorInfoUrl);
}
