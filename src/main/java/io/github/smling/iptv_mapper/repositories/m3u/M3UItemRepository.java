package io.github.smling.iptv_mapper.repositories.m3u;

import io.github.smling.iptv_mapper.models.dao.m3u.M3UItemEntity;
import io.github.smling.iptv_mapper.models.dto.m3u.M3UPlaylistLineView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface M3UItemRepository extends JpaRepository<M3UItemEntity, UUID> {
    List<M3UItemEntity> findByPlaylist_Id(UUID playlistId);

    Page<M3UItemEntity> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    long deleteByPlaylist_Id(UUID playlistId);

    /**
     * Find an item by playlist ID and URL.
     * Uses native query to handle URI stored as text.
     */
    @Query(value = """
        SELECT *
          FROM m3u_item i
         WHERE i.playlist_id = :playlistId
           AND i.url = CAST(:url AS text)
         LIMIT 1
        """, nativeQuery = true)
    Optional<M3UItemEntity> findByPlaylistIdAndUrl(
            @Param("playlistId") UUID playlistId,
            @Param("url") String url
    );

    @Query(value = """
        SELECT
           m.id 						 as tvgChno,
           mi.title                      AS channelName,
           mi.tvg_name 			         AS tvgName,
           mi.tvg_name                   AS tvgId,        -- adjust if you store tvg_id
           mi.tvg_logo                   AS tvgLogo,
           mi.url                        AS streamUrl
        FROM m3u_item_channel_map m
        JOIN m3u_item mi  ON mi.id = m.m3u_item_id
        JOIN channel c    ON c.id = m.channel_id
        ORDER BY tvgName
        """, nativeQuery = true)
    List<M3UPlaylistLineView> findPlaylistLinesAll();

    /**
     * Fetch all M3U items that currently have no mapping (manual or auto).
     */
    @Query("""
        select i
          from M3UItemEntity i
          left join io.github.smling.iptv_mapper.models.dao.M3UItemChannelMapEntity m
                 on m.item = i
         where m is null
        """)
    List<M3UItemEntity> findAllUnmapped();
}
