package io.github.smling.iptv_mapper.models.dao.epg;

import io.github.smling.iptv_mapper.ListUtil;
import io.github.smling.iptv_mapper.StringUtil;
import io.github.smling.iptv_mapper.models.dao.AuditEntity;
import io.github.smling.iptv_mapper.models.dto.epg.Channel;
import io.github.smling.iptv_mapper.models.dto.epg.Text;
import io.github.smling.iptv_mapper.models.dto.epg.UrlRef;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;

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

    // Flexible metadata to align with DTO (e.g., icons)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> meta = new HashMap<>();


    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgrammeEntity> programmes = new ArrayList<>();

    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChannelDisplayNameEntity> displayNames = new ArrayList<>();

    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChannelUrlEntity> urls = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "tv_id")
    private TvEntity tv;

    public ChannelEntity() {}

    /** Factory matching the DTO */
    public static ChannelEntity of(TvEntity tvEntity, Channel dto) {
        ChannelEntity ce = new ChannelEntity()
                .setTv(tvEntity)
                .setChannelId(dto.id());
        // Populate related names
        if (ListUtil.notNullAndNotEmpty(dto.displayNames())) {
            ce.getDisplayNames().clear();
            int pos = 0;
            for (Text t : dto.displayNames()) {
                ChannelDisplayNameEntity cde = new ChannelDisplayNameEntity()
                        .setChannel(ce)
                        .setLang(t.lang())
                        .setName(t.value())
                        .setPosition(pos++);
                ce.getDisplayNames().add(cde);
            }
            // Also set a default displayName to the first value for quick access
            Text first = dto.displayNames().getFirst();
            if (first != null && StringUtil.notNullAndNotEmpty(first.value())) {
                ce.setDisplayName(first.value());
            }
        }
        if(ListUtil.notNullAndNotEmpty(dto.urls())) {
            ce.getUrls().clear();
            for (UrlRef u : dto.urls()) {
                ChannelUrlEntity cue = new ChannelUrlEntity()
                        .setChannel(ce)
                        .setSystem(u.system())
                        .setUrl(u.value());
                ce.getUrls().add(cue);
            }
        }
        // Store icons and other extras into JSONB meta for flexibility
        Map<String, Object> meta = new HashMap<>();
        if (dto.icons() != null && !dto.icons().isEmpty()) {
            meta.put("icons", dto.icons()); // keep structure to preserve attributes
        }
        ce.setMeta(meta);
        return ce;
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

    public Map<String, Object> getMeta() { return meta; }
    public ChannelEntity setMeta(Map<String, Object> meta) { this.meta = (meta != null ? meta : new HashMap<>()); return this; }


    public List<ProgrammeEntity> getProgrammes() { return programmes; }
    public ChannelEntity setProgrammes(List<ProgrammeEntity> programmes) { this.programmes = programmes; return this; }

    public List<ChannelDisplayNameEntity> getDisplayNames() { return displayNames; }
    public ChannelEntity setDisplayNames(List<ChannelDisplayNameEntity> displayNames) { this.displayNames = displayNames; return this; }

    public List<ChannelUrlEntity> getUrls() { return urls; }
    public ChannelEntity setUrls(List<ChannelUrlEntity> urls) { this.urls = urls; return this; }

    public TvEntity getTv() {
        return tv;
    }

    public ChannelEntity setTv(TvEntity tv) {
        this.tv = tv;
        return this;
    }
}
