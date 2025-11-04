package io.github.smling.iptv_mapper.repositories.epg;

import io.github.smling.iptv_mapper.models.dao.epg.ProgrammeEntity;
import io.github.smling.iptv_mapper.models.dto.epg.ProgrammeLineView;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ProgrammeRepository extends JpaRepository<ProgrammeEntity, UUID> {
    @Query(value = """
        SELECT map.id                 AS channelXmltvId,
               p.start_time         AS startUtc,
               p.stop_time          AS stopUtc,
               p.title                AS title,
               p.description          AS desc
          FROM programme p
          JOIN channel c ON p.channel_id = c.id
          JOIN m3u_item_channel_map map ON map.channel_id = c.id
         WHERE p.start_time < :to
           AND p.stop_time  > :from
         ORDER BY c.display_name, p.start_time
        """, nativeQuery = true)
    List<ProgrammeLineView> findProgrammesBetween(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM programme p WHERE p.start_time < :cutoff", nativeQuery = true)
    int deleteOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
