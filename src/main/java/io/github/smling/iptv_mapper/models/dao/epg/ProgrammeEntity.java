package io.github.smling.iptv_mapper.models.dao.epg;

import io.github.smling.iptv_mapper.models.dao.AuditEntity;
import io.github.smling.iptv_mapper.models.dto.epg.Programme;
import io.github.smling.iptv_mapper.parsers.EPGTimeParser;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "programme")
public class ProgrammeEntity extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "stop_time", nullable = false)
    private OffsetDateTime stopTime;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", columnDefinition = "jsonb")
    private java.util.Map<String, Object> meta = new java.util.HashMap<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "channel_id")
    ChannelEntity channel;

    /* ----------  JPA boilerplate omitted ---------- */

    /**
     * Factory that matches the DTO
     */
    public static ProgrammeEntity of(ChannelEntity channelEntity, Programme dto) {
        return new ProgrammeEntity()
                .setChannel(channelEntity)
                .setStartTime(EPGTimeParser.parse(dto.start()))
                .setStopTime(EPGTimeParser.parse(dto.stop()))
                .setTitle(dto.title())
                .setDescription(dto.desc())
                ;
    }

    public UUID getId() {
        return id;
    }

    public ProgrammeEntity setId(UUID id) {
        this.id = id;
        return this;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public ProgrammeEntity setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public OffsetDateTime getStopTime() {
        return stopTime;
    }

    public ProgrammeEntity setStopTime(OffsetDateTime stopTime) {
        this.stopTime = stopTime;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public ProgrammeEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ProgrammeEntity setDescription(String description) {
        this.description = description;
        return this;
    }

    public java.util.Map<String, Object> getMeta() { return meta; }
    public ProgrammeEntity setMeta(java.util.Map<String, Object> meta) { this.meta = meta; return this; }

    public ChannelEntity getChannel() {
        return channel;
    }

    public ProgrammeEntity setChannel(ChannelEntity channel) {
        this.channel = channel;
        return this;
    }
}
