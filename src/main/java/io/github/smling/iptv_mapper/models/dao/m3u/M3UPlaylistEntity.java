package io.github.smling.iptv_mapper.models.dao.m3u;

import io.github.smling.iptv_mapper.models.dao.AuditEntity;
import io.github.smling.iptv_mapper.models.dao.DataSourceEntity;
import io.github.smling.iptv_mapper.models.dto.m3u.M3UPlaylist;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "m3u_playlist")
public class M3UPlaylistEntity extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, String> globalAttributes = new HashMap<>();

    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<M3UItemEntity> items = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "data_source_id", nullable = false)
    private DataSourceEntity dataSource;

    // createdAt / updatedAt inherited from AuditEntity

    // JPA needs this
    protected M3UPlaylistEntity() {}

    private M3UPlaylistEntity(DataSourceEntity dataSource,
                              Map<String, String> globalAttributes,
                              List<M3UItemEntity> items) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource is required");
        if (globalAttributes != null) this.globalAttributes.putAll(globalAttributes);
        if (items != null) items.forEach(this::addItem); // ensures back-reference
    }

    /** Relationship helpers (keep both sides in sync) */
    public void addItem(M3UItemEntity item) {
        items.add(item);
        item.setPlaylist(this);
    }

    public void removeItem(M3UItemEntity item) {
        items.remove(item);
        item.setPlaylist(null);
    }

    // Getters
    public UUID getId() { return id; }
    public Map<String, String> getGlobalAttributes() { return globalAttributes; }
    public List<M3UItemEntity> getItems() { return items; }
    public DataSourceEntity getDataSource() { return dataSource; }

    public M3UPlaylistEntity setId(UUID id) {
        this.id = id;
        return this;
    }

    public M3UPlaylistEntity setGlobalAttributes(Map<String, String> globalAttributes) {
        this.globalAttributes = globalAttributes;
        return this;
    }

    public M3UPlaylistEntity setItems(List<M3UItemEntity> items) {
        this.items = items;
        return this;
    }

    public M3UPlaylistEntity setDataSource(DataSourceEntity dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    /** Factory method */
    public static M3UPlaylistEntity of(DataSourceEntity dataSource,
                                       M3UPlaylist m3UPlaylist
    ) {
        return of(dataSource, m3UPlaylist.globalAttributes());
    }

    public static M3UPlaylistEntity of(DataSourceEntity dataSource,
                                       Map<String, String> globalAttributes
                                       ) {
        return new M3UPlaylistEntity(dataSource, globalAttributes, List.of());
    }

    /**
     * Compares this entity's global attributes with another playlist's.
     * Returns true if they have exactly the same key–value pairs.
     */
    public boolean hasSameGlobalAttributes(M3UPlaylist other) {
        if (other == null || other.globalAttributes() == null) {
            return this.globalAttributes == null || this.globalAttributes.isEmpty();
        }

        // Normalize: treat null/empty as equivalent
        if (this.globalAttributes == null || this.globalAttributes.isEmpty()) {
            return other.globalAttributes().isEmpty();
        }

        return Objects.equals(this.globalAttributes, other.globalAttributes());
    }

    /**
     * Optionally, an overload to compare with another entity.
     */
    public boolean hasSameGlobalAttributes(M3UPlaylistEntity other) {
        if (other == null) return false;
        return Objects.equals(this.globalAttributes, other.globalAttributes);
    }
}
