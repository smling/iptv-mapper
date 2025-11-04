package io.github.smling.iptv_mapper;

import io.github.smling.iptv_mapper.models.dao.epg.ChannelEntity;
import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;
import org.simmetrics.builders.StringMetricBuilder;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public final class FuzzyChannelMatcherSimmetrics {

    private FuzzyChannelMatcherSimmetrics() {}

    // --- Metric: Jaro-Winkler with normalization ---
    private static final StringMetric METRIC = StringMetricBuilder
            .with(StringMetrics.jaroWinkler())
            .simplify(FuzzyChannelMatcherSimmetrics::normalize)
            .build();

    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");

    /**
     * Finds the most similar ChannelEntity by comparing the query with tvgName/displayName.
     * @param query channel name from playlist
     * @param data list of channels
     * @return best matching channel with score
     */
    public static Optional<MatchResult> match(String query, List<ChannelEntity> data) {
        return match(query, data, 0.85);
    }

    public static Optional<MatchResult> match(String query, List<ChannelEntity> data, double threshold) {
        if (query == null || data == null || data.isEmpty()) return Optional.empty();
        return data.stream()
                .map(ch -> {
                    String candidate = firstNonBlank(ch.getDisplayName(), ch.getChannelId());
                    if (candidate == null) return null;
                    float score = METRIC.compare(query, candidate);
                    return new MatchResult(ch, score);
                })
                .filter(Objects::nonNull)
                .filter(r -> r.score() >= threshold)
                .max(Comparator.comparingDouble(MatchResult::score));
    }

    public static List<MatchResult> topN(String query, List<ChannelEntity> data, int n) {
        return data.stream()
                .map(ch -> {
                    String candidate = firstNonBlank(ch.getDisplayName(), ch.getChannelId());
                    if (candidate == null) return null;
                    float score = METRIC.compare(query, candidate);
                    return new MatchResult(ch, score);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(MatchResult::score).reversed())
                .limit(Math.max(1, n))
                .toList();
    }

    // --- helpers ---

    public record MatchResult(ChannelEntity entity, float score) { }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }

    /** Normalize: lowercase, remove accents, replace punctuation with spaces */
    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", ""); // remove accents
        n = NON_ALNUM.matcher(n).replaceAll(" ")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s{2,}", " ");
        return n;
    }
}
