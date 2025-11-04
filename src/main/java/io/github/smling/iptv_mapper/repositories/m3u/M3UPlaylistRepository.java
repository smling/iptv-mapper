package io.github.smling.iptv_mapper.repositories.m3u;

import io.github.smling.iptv_mapper.models.dao.m3u.M3UPlaylistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface M3UPlaylistRepository extends JpaRepository<M3UPlaylistEntity, UUID> {
    List<M3UPlaylistEntity> findByDataSource_Id(UUID dataSourceId);

    boolean existsByDataSource_Id(UUID dataSourceId);

    long deleteByDataSource_Id(UUID dataSourceId);

    /**
     * Find one playlist by DataSource ID and identical global_attributes (JSONB equality).
     */
    @Query(value = """
        SELECT *
          FROM m3u_playlist p
         WHERE p.global_attributes = CAST(:attrs AS jsonb)
         LIMIT 1
        """, nativeQuery = true)
    Optional<M3UPlaylistEntity> findByGlobalAttributes(
            @Param("attrs") String attrsJson
    );
}
