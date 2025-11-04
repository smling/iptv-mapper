package io.github.smling.iptv_mapper.models.dao.epg;


import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "episode_number")
public record EpisodeNumberEntity(
        @Id @GeneratedValue(strategy = GenerationType.UUID)
        UUID id,

        @ManyToOne(optional = false) @JoinColumn(name = "programme_id")
        ProgrammeEntity programme,

        String system,
        String value
) {}