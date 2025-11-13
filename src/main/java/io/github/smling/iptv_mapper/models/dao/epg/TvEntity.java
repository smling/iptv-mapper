package io.github.smling.iptv_mapper.models.dao.epg;

import io.github.smling.iptv_mapper.models.dao.AuditEntity;
import io.github.smling.iptv_mapper.models.dao.DataSourceEntity;
import io.github.smling.iptv_mapper.models.dto.epg.Tv;
import jakarta.persistence.*;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tv")
public class TvEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    String generatorInfoName;
    String generatorInfoUrl;


    @Column(name = "url_checker_result")
    String urlCheckerResult;

    @Column(name = "url_checker_ms")
    Long urlCheckerMs;

    @OneToMany(mappedBy = "tv", cascade = CascadeType.ALL, orphanRemoval = true)
    List<ChannelEntity> channels;

    @ManyToOne(optional = false)
    @JoinColumn(name = "data_source_id")
    DataSourceEntity dataSource;

    // createdAt / updatedAt inherited from AuditEntity

    public static TvEntity of(DataSourceEntity dataSource, Tv tv, Clock clock) {
        return new TvEntity()
                .setDataSource(dataSource)
                .setGeneratorInfoName(tv.generatorInfoName())
                .setGeneratorInfoUrl(tv.generatorInfoUrl());
    }

    public UUID getId() {
        return id;
    }

    public TvEntity setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getGeneratorInfoName() {
        return generatorInfoName;
    }

    public TvEntity setGeneratorInfoName(String generatorInfoName) {
        this.generatorInfoName = generatorInfoName;
        return this;
    }

    public String getGeneratorInfoUrl() {
        return generatorInfoUrl;
    }

    public TvEntity setGeneratorInfoUrl(String generatorInfoUrl) {
        this.generatorInfoUrl = generatorInfoUrl;
        return this;
    }


    public String getUrlCheckerResult() { return urlCheckerResult; }
    public TvEntity setUrlCheckerResult(String urlCheckerResult) { this.urlCheckerResult = urlCheckerResult; return this; }

    public Long getUrlCheckerMs() { return urlCheckerMs; }
    public TvEntity setUrlCheckerMs(Long urlCheckerMs) { this.urlCheckerMs = urlCheckerMs; return this; }

    public List<ChannelEntity> getChannels() {
        return channels;
    }

    public TvEntity setChannels(List<ChannelEntity> channels) {
        this.channels = channels;
        return this;
    }

    public DataSourceEntity getDataSource() {
        return dataSource;
    }

    public TvEntity setDataSource(DataSourceEntity dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public OffsetDateTime getCreatedAt() { return super.getCreatedAt(); }
    public OffsetDateTime getUpdatedAt() { return super.getUpdatedAt(); }
}
