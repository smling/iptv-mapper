package io.github.smling.iptv_mapper.models.dao.epg;

import io.github.smling.iptv_mapper.models.dao.AuditEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.smling.iptv_mapper.models.dto.epg.Channel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "channel")
public class ChannelEntity extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "channel_id", unique = true, nullable = false)
    private String channelId;

    @Column(name = "display_name")
    private String displayName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", columnDefinition = "jsonb")
    private java.util.Map<String, Object> meta = new java.util.HashMap<>();

    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgrammeEntity> programmes = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "tv_id")
    private TvEntity tv;

    public ChannelEntity() {}

    /** Factory matching the DTO */
    public static ChannelEntity of(TvEntity tvEntity, Channel dto) {
        String dn = null;
        if (dto.displayNames() != null && !dto.displayNames().isEmpty()) {
            dn = dto.displayNames().get(0);
        }
        return new ChannelEntity()
                .setTv(tvEntity)
                .setChannelId(dto.id())
                .setDisplayName(dn);
    }

    public void addProgramme(ProgrammeEntity programme) {
        programmes.add(programme);
    }

    /* --- Getters / Setters --- */

    public UUID getId() { return id; }
    public ChannelEntity setId(UUID id) { this.id = id; return this; }

    public String getChannelId() { return channelId; }
    public ChannelEntity setChannelId(String channelId) { this.channelId = channelId; return this; }

    public String getDisplayName() { return displayName; }
    public ChannelEntity setDisplayName(String displayName) { this.displayName = displayName; return this; }

    public java.util.Map<String, Object> getMeta() { return meta; }
    public ChannelEntity setMeta(java.util.Map<String, Object> meta) { this.meta = meta; return this; }

    public List<ProgrammeEntity> getProgrammes() { return programmes; }
    public ChannelEntity setProgrammes(List<ProgrammeEntity> programmes) { this.programmes = programmes; return this; }

    public TvEntity getTv() {
        return tv;
    }

    public ChannelEntity setTv(TvEntity tv) {
        this.tv = tv;
        return this;
    }
}
