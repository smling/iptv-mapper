package io.github.smling.iptv_mapper.models.dao;

import io.github.smling.iptv_mapper.models.DataSourceType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(
        name = "data_source",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_data_source_kind_url",
                columnNames = {"type", "url"}
        )
)
public class DataSourceEntity extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)           // tell Hibernate it's a DB enum
    @Column(nullable = false, name = "type", columnDefinition = "data_source_type") // your PG enum name
    private DataSourceType type;

    @Column(nullable = false, columnDefinition = "text")
    private String url;

    private String label;

    @Column(name = "country_code")
    private String countryCode;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int priority;

    @Column(columnDefinition = "text")
    private String notes;

    private Integer lastHttpStatus;
    private OffsetDateTime lastFetchedAt;
    private String lastEtag;
    private String lastModifiedHdr;
    private String contentChecksum;

    // createdAt / updatedAt inherited from AuditEntity

    public UUID getId() {
        return id;
    }

    public DataSourceEntity setId(UUID id) {
        this.id = id;
        return this;
    }

    public DataSourceType getType() {
        return type;
    }

    public DataSourceEntity setType(DataSourceType type) {
        this.type = type;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public DataSourceEntity setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public DataSourceEntity setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public DataSourceEntity setCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public DataSourceEntity setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public DataSourceEntity setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public String getNotes() {
        return notes;
    }

    public DataSourceEntity setNotes(String notes) {
        this.notes = notes;
        return this;
    }

    public Integer getLastHttpStatus() {
        return lastHttpStatus;
    }

    public DataSourceEntity setLastHttpStatus(Integer lastHttpStatus) {
        this.lastHttpStatus = lastHttpStatus;
        return this;
    }

    public OffsetDateTime getLastFetchedAt() {
        return lastFetchedAt;
    }

    public DataSourceEntity setLastFetchedAt(OffsetDateTime lastFetchedAt) {
        this.lastFetchedAt = lastFetchedAt;
        return this;
    }

    public String getLastEtag() {
        return lastEtag;
    }

    public DataSourceEntity setLastEtag(String lastEtag) {
        this.lastEtag = lastEtag;
        return this;
    }

    public String getLastModifiedHdr() {
        return lastModifiedHdr;
    }

    public DataSourceEntity setLastModifiedHdr(String lastModifiedHdr) {
        this.lastModifiedHdr = lastModifiedHdr;
        return this;
    }

    public String getContentChecksum() {
        return contentChecksum;
    }

    public DataSourceEntity setContentChecksum(String contentChecksum) {
        this.contentChecksum = contentChecksum;
        return this;
    }

    // getCreatedAt / getUpdatedAt provided by AuditEntity

    /**
     * Required by JPA.
     * Keep it protected to avoid accidental usage.
     */
    protected DataSourceEntity() {
    }

    public DataSourceEntity(
            UUID id,
            DataSourceType type,
            String url,
            String label,
            String countryCode,
            boolean enabled,
            int priority,
            String notes,
            Integer lastHttpStatus,
            OffsetDateTime lastFetchedAt,
            String lastEtag,
            String lastModifiedHdr,
            String contentChecksum
    ) {
        this.id = id;
        this.type = type;
        this.url = url;
        this.label = label;
        this.countryCode = countryCode;
        this.enabled = enabled;
        this.priority = priority;
        this.notes = notes;
        this.lastHttpStatus = lastHttpStatus;
        this.lastFetchedAt = lastFetchedAt;
        this.lastEtag = lastEtag;
        this.lastModifiedHdr = lastModifiedHdr;
        this.contentChecksum = contentChecksum;
    }

    // --- Factory Methods ---

    /**
     * Generic factory (used by service layer or tests)
     */
    public static DataSourceEntity of(
            UUID id,
            DataSourceType type,
            String url,
            String label,
            String countryCode,
            boolean enabled,
            int priority,
            String notes,
            Integer lastHttpStatus,
            OffsetDateTime lastFetchedAt,
            String lastEtag,
            String lastModifiedHdr,
            String contentChecksum
    ) {
        return new DataSourceEntity(
                id, type, url, label, countryCode,
                enabled, priority, notes,
                lastHttpStatus, lastFetchedAt,
                lastEtag, lastModifiedHdr, contentChecksum
        );
    }
}
