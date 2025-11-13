package io.github.smling.iptv_mapper.models.dao.epg;

import io.github.smling.iptv_mapper.models.dao.AuditEntity;
import io.github.smling.iptv_mapper.models.dto.epg.Programme;
import io.github.smling.iptv_mapper.models.dto.epg.Text;
import io.github.smling.iptv_mapper.parsers.EPGTimeParser;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    // Flexible metadata captured from DTO fields (XMLTV extras)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> meta = new HashMap<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "channel_id")
    ChannelEntity channel;

    /**
     * Static factory aligning DAO with DTO. Stores extra fields under JSONB 'meta'.
     */
    public static ProgrammeEntity of(ChannelEntity channelEntity, Programme dto) {
        String title = null;
        if (dto.titles() != null && !dto.titles().isEmpty()) {
            title = dto.titles().get(0).value();
        }
        String desc = null;
        if (dto.descs() != null && !dto.descs().isEmpty()) {
            desc = dto.descs().get(0).value();
        }

        Map<String, Object> meta = new HashMap<>();
        // copy optional attributes
        putIfNotNull(meta, "pdcStart", dto.pdcStart());
        putIfNotNull(meta, "vpsStart", dto.vpsStart());
        putIfNotNull(meta, "showview", dto.showview());
        putIfNotNull(meta, "videoplus", dto.videoplus());
        putIfNotNull(meta, "clumpidx", dto.clumpidx());

        // copy multi-valued/simple elements (keep structure lightweight)
        putIfNotEmpty(meta, "subTitles", toValues(dto.subTitles()));
        putIfNotNull(meta, "credits", dto.credits());
        putIfNotNull(meta, "date", dto.date());
        putIfNotEmpty(meta, "categories", toValues(dto.categories()));
        putIfNotEmpty(meta, "keywords", toValues(dto.keywords()));
        putIfNotEmpty(meta, "languages", toValues(dto.languages()));
        if (dto.origLanguage() != null) putIfNotNull(meta, "origLanguage", dto.origLanguage().value());
        putIfNotNull(meta, "length", dto.length());
        putIfNotNull(meta, "icons", dto.icons());
        putIfNotNull(meta, "urls", dto.urls());
        putIfNotNull(meta, "countries", dto.countries());
        putIfNotNull(meta, "episodeNums", dto.episodeNums());
        putIfNotNull(meta, "video", dto.video());
        putIfNotNull(meta, "audio", dto.audio());
        putIfNotNull(meta, "previouslyShown", dto.previouslyShown());
        if (dto.premiere() != null) putIfNotNull(meta, "premiere", dto.premiere().value());
        if (dto.lastChance() != null) putIfNotNull(meta, "lastChance", dto.lastChance().value());
        if (dto.isNew() != null) putIfNotNull(meta, "isNew", true);
        putIfNotNull(meta, "subtitles", dto.subtitles());
        putIfNotNull(meta, "ratings", dto.ratings());
        putIfNotNull(meta, "starRatings", dto.starRatings());
        putIfNotNull(meta, "reviews", dto.reviews());
        putIfNotNull(meta, "images", dto.images());

        return new ProgrammeEntity()
                .setChannel(channelEntity)
                .setStartTime(EPGTimeParser.parse(dto.start()))
                .setStopTime(EPGTimeParser.parse(dto.stop()))
                .setTitle(title)
                .setDescription(desc)
                .setMeta(meta);
    }

    private static void putIfNotNull(Map<String, Object> m, String key, Object value) {
        if (value != null) m.put(key, value);
    }

    private static void putIfNotEmpty(Map<String, Object> m, String key, List<?> values) {
        if (values != null && !values.isEmpty()) m.put(key, values);
    }

    private static List<String> toValues(List<Text> texts) {
        return (texts == null) ? null : texts.stream().filter(Objects::nonNull).map(Text::value).toList();
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

    public Map<String, Object> getMeta() {
        return meta;
    }

    public ProgrammeEntity setMeta(Map<String, Object> meta) {
        this.meta = (meta != null ? meta : new HashMap<>());
        return this;
    }

    public ChannelEntity getChannel() {
        return channel;
    }

    public ProgrammeEntity setChannel(ChannelEntity channel) {
        this.channel = channel;
        return this;
    }
}
