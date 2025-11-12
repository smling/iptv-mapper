package io.github.smling.iptv_mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.smling.iptv_mapper.factories.HttpClientFactory;
import io.github.smling.iptv_mapper.factories.XmlMapperFactory;
import io.github.smling.iptv_mapper.models.dto.epg.Tv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public class EPGClient {

    private static final Logger log = LoggerFactory.getLogger(EPGClient.class);
    private final int HTTP_REQUEST_TIMEOUT_IN_SECOND = 120;
    private final HttpClient httpClient = HttpClientFactory.of();
    private static final ObjectMapper xmlMapper = XmlMapperFactory.ofEPG();

    public Tv fetchEpg(URI url) throws IOException, InterruptedException {
        Instant start = Instant.now();
        log.debug("📡 Fetching EPG: {}", url);

        try {
            Tv result = isGzipUrl(url) ? fetchFromGzip(url) : fetchFromXml(url);
            log.debug("✅ Completed EPG fetch for {} in {} ms", url,
                    Duration.between(start, Instant.now()).toMillis());
            return result;
        } catch (Exception ex) {
            log.error("❌ Error fetching EPG from {}: {}", url, ex.getMessage(), ex);
            throw ex;
        }
    }

    private Tv fetchFromXml(URI url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .timeout(Duration.ofSeconds(HTTP_REQUEST_TIMEOUT_IN_SECOND))
                .header("Accept-Encoding", "gzip, identity")
                .GET()
                .build();

        Instant start = Instant.now();
        log.debug("🌐 Sending XML request to {}", url);

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        long duration = Duration.between(start, Instant.now()).toMillis();
        log.debug("📥 XML response from {} - status: {}, took {} ms",
                url, response.statusCode(), duration);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch EPG (XML), HTTP " + response.statusCode());
        }

        boolean gzip = response.headers().firstValue("Content-Encoding")
                .map(v -> v.toLowerCase(Locale.ROOT).contains("gzip"))
                .orElse(false);

        try (InputStream raw = response.body();
             InputStream in = gzip ? new GZIPInputStream(raw) : raw) {
            Tv tv = xmlMapper.readValue(in, Tv.class);
            log.debug("🧩 Deserialized XML into Tv object from {}", url);
            return tv;
        } catch (Exception ex) {
            log.error("⚠️ Error deserializing XML from {}: {}", url, ex.getMessage(), ex);
            throw ex;
        }
    }

    private Tv fetchFromGzip(URI url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .timeout(Duration.ofSeconds(HTTP_REQUEST_TIMEOUT_IN_SECOND))
                .GET()
                .build();

        Instant start = Instant.now();
        log.debug("🌐 Sending GZIP request to {}", url);

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        long duration = Duration.between(start, Instant.now()).toMillis();
        log.debug("📥 GZIP response from {} - status: {}, size: {} bytes, took {} ms",
                url, response.statusCode(), response.body().length, duration);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch EPG (GZIP), HTTP " + response.statusCode());
        }

        String expectedXmlName = deriveXmlNameFromGz(url);
        log.debug("📦 Expected XML file name derived from GZ: {}", expectedXmlName);

        try (GZIPInputStream gzin = new GZIPInputStream(new ByteArrayInputStream(response.body()))) {
            Tv tv = xmlMapper.readValue(gzin, Tv.class);
            log.debug("🧩 Deserialized GZIP XML into Tv object from {}", url);
            return tv;
        } catch (Exception ex) {
            log.error("⚠️ Error decompressing or deserializing GZIP from {}: {}", url, ex.getMessage(), ex);
            throw ex;
        }
    }

    private boolean isGzipUrl(URI url) {
        boolean gzip = url.getPath().toLowerCase(Locale.ROOT).endsWith(".gz");
        log.debug("🔍 URL {} detected as {}", url, gzip ? "GZIP file" : "plain XML");
        return gzip;
    }

    private String deriveXmlNameFromGz(URI url) {
        String path = url.getPath();
        int lastSlash = path.lastIndexOf('/');
        String file = (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;

        String derived;
        if (file.toLowerCase(Locale.ROOT).endsWith(".xml.gz")) {
            derived = file.substring(0, file.length() - ".gz".length());
        } else if (file.toLowerCase(Locale.ROOT).endsWith(".gz")) {
            derived = file.substring(0, file.length() - ".gz".length()) + ".xml";
        } else {
            derived = file;
        }

        log.debug("🧮 Derived XML name from GZIP: {} -> {}", file, derived);
        return derived;
    }

    /**
     * Open an InputStream to the EPG XML for streaming consumption.
     * Caller is responsible for closing the returned stream.
     */
    public InputStream openXmlStream(URI url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .timeout(Duration.ofSeconds(HTTP_REQUEST_TIMEOUT_IN_SECOND))
                .header("Accept-Encoding", "gzip, identity")
                .GET()
                .build();

        Instant start = Instant.now();
        log.debug("🌐 Opening XML stream to {}", url);

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        long duration = Duration.between(start, Instant.now()).toMillis();
        log.debug("📥 XML stream from {} - status: {}, took {} ms",
                url, response.statusCode(), duration);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to open EPG stream (XML), HTTP " + response.statusCode());
        }

        boolean gzip = response.headers().firstValue("Content-Encoding")
                .map(v -> v.toLowerCase(Locale.ROOT).contains("gzip"))
                .orElse(false);

        InputStream raw = response.body();
        return gzip ? new GZIPInputStream(raw) : raw;
    }

    /**
     * Download the EPG (decompressing if needed) to a temporary file and return its path.
     * This decouples network I/O from downstream parsing/DB I/O to avoid server-side timeouts
     * and premature EOF when parsing slowly.
     */
    public java.nio.file.Path downloadToTempFile(URI url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .timeout(Duration.ofSeconds(HTTP_REQUEST_TIMEOUT_IN_SECOND))
                .header("Accept-Encoding", "gzip, identity")
                .GET()
                .build();

        Instant start = Instant.now();
        log.debug("🌐 Downloading EPG to temp file: {}", url);

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        long duration = Duration.between(start, Instant.now()).toMillis();
        log.debug("📥 Response {} - status: {}, took {} ms", url, response.statusCode(), duration);

        if (response.statusCode() != 200) {
            throw new IOException("Failed to download EPG (XML), HTTP " + response.statusCode());
        }

        boolean gzip = response.headers().firstValue("Content-Encoding")
                .map(v -> v.toLowerCase(Locale.ROOT).contains("gzip"))
                .orElse(false);

        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("epg-", ".xml");
        try (InputStream raw = response.body();
             InputStream in = gzip ? new GZIPInputStream(raw) : raw;
             java.io.OutputStream out = java.nio.file.Files.newOutputStream(tmp, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
            in.transferTo(out);
        }
        log.debug("💾 EPG saved to {} (gzip={})", tmp, gzip);
        return tmp;
    }
}
