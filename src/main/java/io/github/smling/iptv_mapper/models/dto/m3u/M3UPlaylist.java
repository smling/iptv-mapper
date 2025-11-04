package io.github.smling.iptv_mapper.models.dto.m3u;

import java.util.List;
import java.util.Map;

public record M3UPlaylist(
        Map<String, String> globalAttributes,   // e.g., url-tvg="...", x-tvg-url="..."
        List<M3UItem> items
) {}
