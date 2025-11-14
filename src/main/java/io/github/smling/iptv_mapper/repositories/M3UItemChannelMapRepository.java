package io.github.smling.iptv_mapper.repositories;

import io.github.smling.iptv_mapper.models.dao.M3UItemChannelMapEntity;
import io.github.smling.iptv_mapper.models.dto.m3u.M3UItemChannelView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface M3UItemChannelMapRepository extends JpaRepository<M3UItemChannelMapEntity, UUID> {

    Optional<M3UItemChannelMapEntity> findByItemId(UUID m3uItemId);

    @Query("select m from M3UItemChannelMapEntity m " +
            "where m.item.id = :itemId and m.manual = true")
    Optional<M3UItemChannelMapEntity> findManualByItemId(@Param("itemId") UUID itemId);

    @Query(
            value = """
            SELECT
              muicm.id               AS mapId,
              c.id                   AS channelId,
              mui.id                 AS itemId,
              mui.title              AS title,
              c.display_name         AS displayName,
              mui.url                AS url,
              mui.tvg_logo           AS tvIconUrl,
              mui.tvg_id             AS tvId,
              mui.url_checker_result AS urlCheckerResult,
              muicm.confidence       AS confidence,
              muicm.is_manual        AS manual
            FROM m3u_item mui
            JOIN m3u_item_channel_map muicm ON mui.id  = muicm.m3u_item_id
            JOIN channel c                  ON muicm.channel_id = c.id
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM m3u_item mui
            JOIN m3u_item_channel_map muicm ON mui.id  = muicm.m3u_item_id
            JOIN channel c                  ON muicm.channel_id = c.id
            """,
            nativeQuery = true
    )
    Page<M3UItemChannelView> findAllProjected(Pageable pageable);
}
