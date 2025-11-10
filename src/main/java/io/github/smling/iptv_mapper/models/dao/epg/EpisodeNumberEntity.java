package io.github.smling.iptv_mapper.models.dao.epg;

import io.github.smling.iptv_mapper.models.dao.AuditEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "episode_number")
public class EpisodeNumberEntity extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "programme_id")
    private ProgrammeEntity programme;

    private String system;
    private String value;

    protected EpisodeNumberEntity() {}

    public EpisodeNumberEntity(UUID id, ProgrammeEntity programme, String system, String value) {
        this.id = id;
        this.programme = programme;
        this.system = system;
        this.value = value;
    }

    public UUID getId() { return id; }
    public ProgrammeEntity getProgramme() { return programme; }
    public String getSystem() { return system; }
    public String getValue() { return value; }

    public EpisodeNumberEntity setId(UUID id) { this.id = id; return this; }
    public EpisodeNumberEntity setProgramme(ProgrammeEntity programme) { this.programme = programme; return this; }
    public EpisodeNumberEntity setSystem(String system) { this.system = system; return this; }
    public EpisodeNumberEntity setValue(String value) { this.value = value; return this; }
}
