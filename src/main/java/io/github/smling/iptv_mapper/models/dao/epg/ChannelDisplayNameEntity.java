package io.github.smling.iptv_mapper.models.dao.epg;

import io.github.smling.iptv_mapper.models.dao.AuditEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "channel_display_name")
public class ChannelDisplayNameEntity extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "channel_id")
    private ChannelEntity channel;

    @Column(name = "lang")
    private String lang;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "pos")
    private Integer position;

    public UUID getId() { return id; }
    public ChannelDisplayNameEntity setId(UUID id) { this.id = id; return this; }

    public ChannelEntity getChannel() { return channel; }
    public ChannelDisplayNameEntity setChannel(ChannelEntity channel) { this.channel = channel; return this; }

    public String getLang() { return lang; }
    public ChannelDisplayNameEntity setLang(String lang) { this.lang = lang; return this; }

    public String getName() { return name; }
    public ChannelDisplayNameEntity setName(String name) { this.name = name; return this; }

    public Integer getPosition() { return position; }
    public ChannelDisplayNameEntity setPosition(Integer position) { this.position = position; return this; }
}

