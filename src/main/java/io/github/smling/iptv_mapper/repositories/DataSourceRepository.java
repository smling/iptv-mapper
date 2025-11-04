package io.github.smling.iptv_mapper.repositories;

import io.github.smling.iptv_mapper.models.DataSourceType;
import io.github.smling.iptv_mapper.models.dao.DataSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface DataSourceRepository extends JpaRepository<DataSourceEntity, UUID> {
    Optional<DataSourceEntity> findByTypeAndUrl(DataSourceType type, String url);
    List<DataSourceEntity> findByEnabledTrueOrderByPriorityAsc();
    List<DataSourceEntity> findByTypeOrderByPriorityAsc(DataSourceType type);
}

