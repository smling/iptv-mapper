package io.github.smling.iptv_mapper.parsers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EPGTimeParserTest {

    @ParameterizedTest
    @CsvSource({
            // input                               , expected-ISO
            "'20251030221000 +0000', '2025-10-30T22:10Z'",
            "'20251029075800 +0000', '2025-10-29T07:58Z'",
            // extra coverage with non-zero offsets
            "'20251030221000 +0130', '2025-10-30T20:40Z'",
            "'20251029075800 -0500', '2025-10-29T12:58Z'"
    })
    void parses_to_expected_iso_instant(String input, String expectedIsoUtc) {
        var odt = EPGTimeParser.parse(input);
        // normalize to UTC for comparison with expected string
        String actual = odt.withOffsetSameInstant(ZoneOffset.UTC).toString();
        assertEquals(expectedIsoUtc, actual);
    }

    @ParameterizedTest
    @CsvSource({
            // Accepts without a space as well (optional)
            "'20251030221000+0000', '2025-10-30T22:10Z'",
            "'20251029075800+0000', '2025-10-29T07:58Z'"
    })
    void parses_without_space_between_datetime_and_offset(String input, String expectedIsoUtc) {
        var odt = EPGTimeParser.parse(input);
        String actual = odt.withOffsetSameInstant(ZoneOffset.UTC).toString();
        assertEquals(expectedIsoUtc, actual);
    }
}