package io.github.smling.iptv_mapper.models.dao.epg;

import io.github.smling.iptv_mapper.models.dao.AuditEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "channel_url")
public class ChannelUrlEntity extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "channel_id")
    private ChannelEntity channel;

    @Column(name = "system")
    private String system;

    @Column(name = "url", nullable = false)
    private String url;

    public UUID getId() { return id; }
    public ChannelUrlEntity setId(UUID id) { this.id = id; return this; }

    public ChannelEntity getChannel() { return channel; }
    public ChannelUrlEntity setChannel(ChannelEntity channel) { this.channel = channel; return this; }

    public String getSystem() { return system; }
    public ChannelUrlEntity setSystem(String system) { this.system = system; return this; }

    public String getUrl() { return url; }
    public ChannelUrlEntity setUrl(String url) { this.url = url; return this; }
}

