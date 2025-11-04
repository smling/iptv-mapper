package io.github.smling.iptv_mapper.models.dto.m3u;

import java.net.URI;
import java.time.Duration;
import java.util.*;

/**
 * Represents a single M3U entry parsed from an #EXTINF block.
 */
public record M3UItem(
        Duration duration,         // -1 for “unknown/stream”
        String title,              // title after comma on #EXTINF line
        URI url,                   // media URL on following line
        String tvgId,
        String tvgName,
        URI tvgLogo,
        String groupTitle,
        Map<String, String> extraAttributes // any other unrecognized attributes
) {

    /**
     * Factory that expands common attributes (tvg-id, tvg-name, tvg-logo, group-title)
     * from the raw attribute map.
     */
    public static M3UItem of(Duration duration, String title, URI url, Map<String, String> attributes) {
        if (attributes == null) attributes = Map.of();

        String tvgId = attributes.get("tvg-id");
        String tvgName = attributes.get("tvg-name");
        String groupTitle = attributes.get("group-title");
        URI tvgLogo = Optional.ofNullable(attributes.get("tvg-logo"))
                .filter(s -> !s.isBlank())
                .map(URI::create)
                .orElse(null);

        // Retain unrecognized attributes
        Map<String, String> extras = new LinkedHashMap<>(attributes);
        extras.keySet().removeAll(Set.of("tvg-id", "tvg-name", "tvg-logo", "group-title"));

        return new M3UItem(duration, title, url, tvgId, tvgName, tvgLogo, groupTitle,
                Collections.unmodifiableMap(extras));
    }
}

