package io.github.smling.iptv_mapper.parsers;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public final class EPGTimeParser {

    private EPGTimeParser() {}

    // Pattern like: 20251030221000 +0000  -> yyyyMMddHHmmss Z
    private static final DateTimeFormatter EPG_FORMATTER =
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyyMMddHHmmss")
                    .optionalStart().appendLiteral(' ').optionalEnd()
                    .appendPattern("Z") // +0000 / -0530 etc.
                    .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                    .toFormatter();

    /**
     * Parses strings like "20251030221000 +0000" into OffsetDateTime.
     * @throws java.time.format.DateTimeParseException if the input is invalid.
     */
    public static OffsetDateTime parse(String value) {
        return OffsetDateTime.parse(value, EPG_FORMATTER);
    }

    /** Convenience: returns ISO-8601 string (e.g. 2025-10-30T22:10:00Z). */
    public static String toIsoInstantString(Instant value) {
        return EPG_FORMATTER.format(value.atOffset(ZoneOffset.UTC));
    }
}
