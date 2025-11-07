package io.github.smling.iptv_mapper.parsers;

import io.github.smling.iptv_mapper.models.dto.m3u.M3UItem;
import io.github.smling.iptv_mapper.models.dto.m3u.M3UPlaylist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.*;

public class M3UPlaylistParser {
    private final Logger logger = LoggerFactory.getLogger(M3UPlaylistParser.class);
    /**
     * Parses M3U/M3U8 text into DTOs.
     * Supports common IPTV lists like:
     *   #EXTM3U url-tvg="..." x-tvg-url="..."
     *   #EXTINF:-1 tvg-id="BBC" group-title="News",BBC World News
     *   http://...
     */
    public M3UPlaylist parse(String text, URI baseUri) {
        // Normalise newlines and trim BOM if present.
        String content = stripBom(text).replace("\r\n", "\n").replace("\r", "\n").trim();

        if (!content.startsWith("#EXTM3U")) {
            // M3U should start with #EXTM3U – continue but note that attributes won’t exist at top.
        }

        Map<String, String> globalAttrs = new LinkedHashMap<>();
        List<M3UItem> items = new ArrayList<>();

        // Extract any attributes on the #EXTM3U header line
        {
            int firstNewline = content.indexOf('\n');
            String header = firstNewline >= 0 ? content.substring(0, firstNewline) : content;
            if (header.startsWith("#EXTM3U")) {
                globalAttrs.putAll(parseKeyValueAttributes(header.substring("#EXTM3U".length()).trim()));
            }
        }

        // Iterate lines and collect entries
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#EXTM3U")) continue;

            if (line.startsWith("#EXTINF:")) {
                // Parse EXTINF line -> duration, attributes, title
                ExtInf extInf = parseExtInf(line);

                // Next non-empty, non-tag line should be the media URL (though some playlists allow tags in between)
                URI mediaUri = null;
                int j = i + 1;
                while (j < lines.length) {
                    String next = lines[j].trim();
                    if (next.isEmpty()) { j++; continue; }
                    if (next.startsWith("#")) { j++; continue; } // skip other tags like #EXTVLCOPT, #EXTGRP, etc.
                    try {

                        mediaUri = baseUri.resolve(next);
                        // Validate URI syntax
                        mediaUri.toURL(); // Will throw if invalid
                    } catch (Exception e) {
                        logger.warn("⚠️ [M3UPlaylistParser] Invalid media URI: {} ({}), skipping item.", next, e.getMessage());
                        mediaUri = null;
                    }
                    break;
                }
                if (mediaUri == null) {
                    // No URL found; create a partial item (rare/malformed)
                    items.add(M3UItem.of(extInf.duration(), extInf.title(), null, extInf.attributes()));
                    continue;
                }
                items.add(M3UItem.of(extInf.duration(), extInf.title(), mediaUri, extInf.attributes()));
                // Move cursor to the URL line so outer loop continues afterwards
                i = j;
            }
            // We ignore other tags like #EXT-X-STREAM-INF here, but you can extend similarly if needed.
        }

        return new M3UPlaylist(Collections.unmodifiableMap(globalAttrs), List.copyOf(items));
    }

    // -------- helpers --------

    private static String stripBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    /** Container for parsed EXTINF line. */
    private record ExtInf(Duration duration, String title, Map<String, String> attributes) {}

    /**
     * Parses a line like:
     *   #EXTINF:-1 tvg-id="BBC" tvg-name="BBC World" group-title="News",BBC World News
     * or:
     *   #EXTINF:123,Some Title
     */
    private static ExtInf parseExtInf(String line) {
        // Remove marker
        String rest = line.substring("#EXTINF:".length()).trim();

        // Split once at the last comma to preserve commas in attribute values/titles
        int comma = rest.lastIndexOf(',');
        String left = comma >= 0 ? rest.substring(0, comma).trim() : rest;
        String title = comma >= 0 ? rest.substring(comma + 1).trim() : "";

        // Left side begins with duration, possibly floating (e.g., 10.0) or -1
        // and may include attributes afterwards: duration SP attributes
        String[] firstSplit = left.split("\\s+", 2);
        String durationStr = firstSplit.length > 0 ? firstSplit[0] : "-1";
        Duration duration = parseDurationSeconds(durationStr);

        Map<String, String> attrs = new LinkedHashMap<>();
        if (firstSplit.length == 2) {
            attrs.putAll(parseKeyValueAttributes(firstSplit[1]));
        }

        return new ExtInf(duration, title, attrs);
    }

    private static Duration parseDurationSeconds(String s) {
        // HLS allows integer or float seconds; IPTV often uses -1 as “infinite live”
        try {
            if (s.contains(".")) {
                double d = Double.parseDouble(s);
                long secs = (long) Math.floor(d);
                return Duration.ofSeconds(secs);
            } else {
                long secs = Long.parseLong(s);
                return Duration.ofSeconds(secs);
            }
        } catch (NumberFormatException e) {
            return Duration.ofSeconds(-1);
        }
    }

    /**
     * Linear, regex-free parser for key=value attribute lists.
     * Handles:
     *   key="quoted \"value\""  (supports backslash escapes)
     *   key=value               (unquoted, ends at whitespace or comma)
     *   optional whitespace around '='
     * Treats commas as separators (common in some playlists).
     */
    private static Map<String, String> parseKeyValueAttributes(String s) {
        Map<String, String> map = new LinkedHashMap<>();
        if (s == null || s.isEmpty()) return map;

        int i = 0, n = s.length();

        // Helper lambdas
        java.util.function.IntPredicate isSep = ch -> Character.isWhitespace(ch) || ch == ',';

        while (i < n) {
            // skip separators
            while (i < n && isSep.test(s.charAt(i))) i++;
            if (i >= n) break;

            // read key
            int kStart = i;
            while (i < n) {
                char c = s.charAt(i);
                if (c == '=' || isSep.test(c)) break;
                i++;
            }
            int kEnd = i;
            String key = s.substring(kStart, kEnd).trim();
            if (key.isEmpty()) { // skip junk token safely
                if (i < n && s.charAt(i) != '=') { i++; }
                continue;
            }

            // skip spaces between key and '='
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;

            // expect '='
            if (i >= n || s.charAt(i) != '=') {
                // tolerate bare keys without a value
                map.putIfAbsent(key, "");
                continue;
            }
            i++; // skip '='

            // skip spaces after '='
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;
            if (i >= n) { map.putIfAbsent(key, ""); break; }

            // read value
            String value;
            char ch = s.charAt(i);
            if (ch == '"') {
                i++; // consume opening quote
                StringBuilder sb = new StringBuilder();
                boolean escaped = false;
                while (i < n) {
                    char c = s.charAt(i++);
                    if (escaped) {
                        sb.append(c);
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '"') {
                        break; // closing quote
                    } else {
                        sb.append(c);
                    }
                }
                value = sb.toString();
            } else {
                int vStart = i;
                while (i < n && !isSep.test(s.charAt(i))) i++;
                value = s.substring(vStart, i).trim();
            }

            map.put(key, value);
            // loop continues; i already at next separator or end
        }

        return map;
    }
}
