package io.github.smling.iptv_mapper;

import io.github.smling.iptv_mapper.models.dto.m3u.M3UPlaylist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class M3UClientTest {

    static Stream<URI> urlProvider() {
        return Stream.of(
                URI.create("https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"),
                URI.create("https://iptv-org.github.io/iptv/index.m3u")
        );
    }


    @ParameterizedTest(name = "m3u url {0} should be deserialize")
    @MethodSource("urlProvider")
    @DisplayName("M3UClient.get: m3u url can deserialize")
    void fetch(URI uri, TestReporter reporter) throws IOException, InterruptedException {
        M3UClient client = new M3UClient(uri);
        M3UPlaylist playlist = client.get();

        // Assert basic expectations
        assertNotNull(playlist);
        assertFalse(playlist.items().isEmpty(), "Playlist should contain items");

        // Report to test console
        reporter.publishEntry("Global attributes", playlist.globalAttributes().toString());
        reporter.publishEntry("Item count", String.valueOf(playlist.items().size()));

        // Show a few sample items
        playlist.items().stream().limit(3).forEach(item -> {
            reporter.publishEntry("---- Item ----",
                    "Title=" + item.title() + "\n" +
                            "URL=" + item.url() + "\n" +
                            "Duration=" + item.duration().getSeconds() + "s\n" +
                            "Title=" + item.title());
        });
    }
}