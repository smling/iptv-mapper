package io.github.smling.iptv_mapper.repositories.epg;

import io.github.smling.iptv_mapper.models.dao.epg.ChannelEntity;
import io.github.smling.iptv_mapper.models.dto.epg.ChannelLineView;
import io.github.smling.iptv_mapper.models.dto.epg.ChannelProgrammeView;
import io.github.smling.iptv_mapper.models.dto.m3u.M3UPlaylistLineView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<ChannelEntity, String> {
    /**
     * Fetch channels by data source country without lazy-loading tv/data_source.
     */
    @Query(value = """
        SELECT c.*
          FROM channel c
          JOIN tv t  ON c.tv_id = t.id
          JOIN data_source ds ON t.data_source_id = ds.id
         WHERE ds.country_code = :countryCode
         ORDER BY c.channel_id
        """, nativeQuery = true)
    List<ChannelEntity> findAllByDataSourceCountry(@Param("countryCode") String countryCode);

    @Query(value = """
        SELECT map.id          AS xmltvId,
               c.display_name  AS displayName
          FROM m3u_item_channel_map map
          JOIN channel c ON c.id = map.channel_id
         ORDER BY c.display_name NULLS LAST, c.id
        """, nativeQuery = true)
    List<ChannelLineView> findAllChannelsWithMappingId();

    @Query(value = """
        SELECT c.id          AS channelDbId,
               c.channel_id  AS xmltvId,
               m3uItem.title AS displayName
          FROM m3u_item_channel_map map
          JOIN channel c ON c.id = map.channel_id
          JOIN m3u_item m3uItem on m3uItem.id = map.mcu_item_id
         ORDER BY c.display_name NULLS LAST, c.id
        """, nativeQuery = true)
    List<io.github.smling.iptv_mapper.models.dto.epg.ChannelEpgRow> findAllChannelsForEpg();

    @Query(value = """
        SELECT
          c.id            AS channelDbId,
          c.channel_id    AS channelId,
          p.id            AS programmeId,
          p.start_time    AS startTime,
          p.stop_time     AS stopTime,
          p.title         AS title,
          p.description   AS description,
          mi.m3u_title    AS m3uItemTitle
        FROM channel c
        JOIN programme p ON c.id = p.channel_id
        LEFT JOIN LATERAL (
            SELECT m_inner.title AS m3u_title
            FROM m3u_item_channel_map map
            JOIN m3u_item m_inner ON m_inner.id = map.m3u_item_id
            WHERE map.channel_id = c.id
            ORDER BY map.updated_at DESC NULLS LAST, map.created_at DESC NULLS LAST
            LIMIT 1
        ) mi ON TRUE
        WHERE c.id = :channelDbId
        ORDER BY p.start_time ASC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM channel c
        JOIN programme p ON c.id = p.channel_id
        WHERE c.id = :channelDbId
        """,
            nativeQuery = true)
    Page<ChannelProgrammeView> findByChannelDbId(
            @Param("channelDbId") UUID channelDbId,
            Pageable pageable
    );

    // Upsert helpers
    java.util.Optional<ChannelEntity> findByChannelId(String channelId);
    java.util.Optional<ChannelEntity> findByTv_IdAndChannelId(UUID tvId, String channelId);
}
