package io.github.smling.iptv_mapper.models.dao.m3u;

import io.github.smling.iptv_mapper.models.dao.AuditEntity;
import io.github.smling.iptv_mapper.models.dto.m3u.M3UItem;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import java.util.*;

@Entity
@Table(name = "m3u_item")
public class M3UItemEntity extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private M3UPlaylistEntity playlist;

    // persisted as BIGINT seconds via converter
    @Column(name = "duration_sec")
    private Duration duration;

    private String title;

    // store as TEXT (normalized String)
    private String url;

    // ---- NEW typed columns mapped from DTO attributes ----
    @Column(name = "tvg_id")
    private String tvgId;

    @Column(name = "tvg_name")
    private String tvgName;

    @Column(name = "tvg_logo")   // store URI as String
    private String tvgLogo;

    @Column(name = "group_title")
    private String groupTitle;

    @Column(name = "url_checker_result")
    private String urlCheckerResult;

    @Column(name = "url_checker_ms")
    private Long urlCheckerMs;

    // Deprecated legacy JSON attributes column removed from mapping to avoid duplication.

    /** JPA needs it */
    protected M3UItemEntity() {}

    private M3UItemEntity(
            M3UPlaylistEntity playlist,
            Duration duration,
            String title,
            String url,
            String tvgId,
            String tvgName,
            String tvgLogo,
            String groupTitle
    ) {
        this.playlist = Objects.requireNonNull(playlist, "playlist is required");
        this.duration = duration;
        this.title = title;
        this.url = url;
        this.tvgId = tvgId;
        this.tvgName = tvgName;
        this.tvgLogo = tvgLogo;
        this.groupTitle = groupTitle;
    }

    // ---------- Factory methods ----------

    /** From DTO */
    public static M3UItemEntity of(M3UPlaylistEntity playlist, M3UItem dto) {
        Objects.requireNonNull(dto, "dto is required");
        return of(
                playlist,
                dto.duration(),
                dto.title(),
                dto.url() != null ? dto.url().toString() : null,
                dto.tvgId(),
                dto.tvgName(),
                dto.tvgLogo() != null ? dto.tvgLogo().toString() : null,
                dto.groupTitle()
        );
    }

    /** General factory */
    public static M3UItemEntity of(
            M3UPlaylistEntity playlist,
            Duration duration,
            String title,
            String url,
            String tvgId,
            String tvgName,
            String tvgLogo,
            String groupTitle
    ) {
        return new M3UItemEntity(
                playlist, duration, title, url,
                tvgId, tvgName, tvgLogo, groupTitle
        );
    }

    // ---------- Relationship helper ----------

    void setPlaylist(M3UPlaylistEntity playlist) { this.playlist = playlist; }

    // ---------- Getters / Setters (fluent optional) ----------

    public UUID getId() { return id; }

    public M3UPlaylistEntity getPlaylist() { return playlist; }

    public Duration getDuration() { return duration; }
    public M3UItemEntity setDuration(Duration duration) { this.duration = duration; return this; }

    public String getTitle() { return title; }
    public M3UItemEntity setTitle(String title) { this.title = title; return this; }

    public String getUrl() { return url; }
    public M3UItemEntity setUrl(String url) { this.url = url; return this; }

    public String getTvgId() { return tvgId; }
    public M3UItemEntity setTvgId(String tvgId) { this.tvgId = tvgId; return this; }

    public String getTvgName() { return tvgName; }
    public M3UItemEntity setTvgName(String tvgName) { this.tvgName = tvgName; return this; }

    public String getTvgLogo() { return tvgLogo; }
    public M3UItemEntity setTvgLogo(String tvgLogo) { this.tvgLogo = tvgLogo; return this; }

    public String getGroupTitle() { return groupTitle; }
    public M3UItemEntity setGroupTitle(String groupTitle) { this.groupTitle = groupTitle; return this; }

    public String getUrlCheckerResult() { return urlCheckerResult; }
    public M3UItemEntity setUrlCheckerResult(String urlCheckerResult) { this.urlCheckerResult = urlCheckerResult; return this; }

    public Long getUrlCheckerMs() { return urlCheckerMs; }
    public M3UItemEntity setUrlCheckerMs(Long urlCheckerMs) { this.urlCheckerMs = urlCheckerMs; return this; }

    // attributes JSON column intentionally unmapped/removed
}
