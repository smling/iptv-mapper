package io.github.smling.iptv_mapper;

import io.github.smling.iptv_mapper.factories.HttpClientFactory;
import io.github.smling.iptv_mapper.models.dto.m3u.M3UPlaylist;
import io.github.smling.iptv_mapper.parsers.M3UPlaylistParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class M3UClient {
    private final URI uri;
    private final HttpClient http = HttpClientFactory.of();
    private final Logger logger = LoggerFactory.getLogger(M3UClient.class);

    public M3UClient(URI uri) {
        this.uri = uri;
    }

    /**
     * Returns the playlist, using the cached copy if available; otherwise fetches from the network.
     */
    public M3UPlaylist get() throws IOException, InterruptedException {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(uri)
                    .GET()
                    .header("User-Agent", "M3U8Client/1.0 (Java 17)")
                    .build();
        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ [M3UClient] Invalid URI: {} ({}), skipping.",uri, e.getMessage());
            return null;
        }
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String body = response.body();
            return new M3UPlaylistParser().parse(body, uri);
        }
        throw new IOException("HTTP " + response.statusCode() + " fetching " + uri);
    }
}
