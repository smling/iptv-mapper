package io.github.smling.iptv_mapper.models.dao;

import io.github.smling.iptv_mapper.models.dao.epg.ChannelEntity;
import io.github.smling.iptv_mapper.models.dao.m3u.M3UItemEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "m3u_item_channel_map",
        uniqueConstraints = @UniqueConstraint(name = "uq_item_unique", columnNames = "m3u_item_id"))
public class M3UItemChannelMapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "m3u_item_id", nullable = false)
    private M3UItemEntity item;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private ChannelEntity channel;

    @Column(name = "is_manual", nullable = false)
    private boolean manual;

    @Column(precision = 5, scale = 4)
    private java.math.BigDecimal confidence;   // store 0..1

    private String method;                     // "jaroWinkler"/"cosine"/"admin"

    @Column(name = "created_at", nullable = false)
    private java.time.OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private java.time.OffsetDateTime updatedAt;

    protected M3UItemChannelMapEntity() {}

    public static M3UItemChannelMapEntity ofAuto(M3UItemEntity item, ChannelEntity channel,
                                                 double score, String method, java.time.Clock clock) {
        var now = java.time.OffsetDateTime.now(clock);
        var e = new M3UItemChannelMapEntity();
        e.item = item;
        e.channel = channel;
        e.manual = false;
        e.confidence = java.math.BigDecimal.valueOf(score);
        e.method = method;
        e.createdAt = now;
        e.updatedAt = now;
        return e;
    }

    public static M3UItemChannelMapEntity ofManual(M3UItemEntity item, ChannelEntity channel,
                                                   String noteOrMethod, java.time.Clock clock) {
        var now = java.time.OffsetDateTime.now(clock);
        var e = new M3UItemChannelMapEntity();
        e.item = item;
        e.channel = channel;
        e.manual = true;
        e.method = noteOrMethod != null ? noteOrMethod : "admin";
        e.createdAt = now;
        e.updatedAt = now;
        return e;
    }

    // getters/setters (minimised)
    public UUID getId() { return id; }
    public M3UItemEntity getItem() { return item; }
    public ChannelEntity getChannel() { return channel; }
    public boolean isManual() { return manual; }
    public java.math.BigDecimal getConfidence() { return confidence; }
    public String getMethod() { return method; }
    public java.time.OffsetDateTime getCreatedAt() { return createdAt; }
    public java.time.OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setChannel(ChannelEntity channel) { this.channel = channel; } // allow manual re-point
}
