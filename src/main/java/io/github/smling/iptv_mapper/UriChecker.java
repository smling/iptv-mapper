package io.github.smling.iptv_mapper;

import io.github.smling.iptv_mapper.factories.HttpClientFactory;
import io.github.smling.iptv_mapper.factories.RtspSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class UriChecker {

    private static final Logger log = LoggerFactory.getLogger(UriChecker.class);

    private final HttpClient http;
    private final Duration hardTimeout;
    private final HttpClient.Version httpVersion;
    private final Executor executor;   // for RTSP socket work

    // Accept 2xx/3xx as reachable; 401/403 mean "alive but requires auth" → also acceptable for health-check
    private static boolean isReachableCode(int code) {
        return (code >= 200 && code < 400) || code == 401 || code == 403;
    }

    public UriChecker(HttpClient http, Duration hardTimeout, HttpClient.Version httpVersion, Executor executor) {
        this.http = Objects.requireNonNull(http);
        this.hardTimeout = hardTimeout == null ? Duration.ofSeconds(7) : hardTimeout;
        this.httpVersion = httpVersion == null ? HttpClient.Version.HTTP_1_1 : httpVersion;
        this.executor = executor != null ? executor : ForkJoinPool.commonPool();
    }

    public UriChecker() {
        this(HttpClientFactory.of(),
                Duration.ofSeconds(7),
                HttpClient.Version.HTTP_1_1,
                ForkJoinPool.commonPool());
    }

    /** Async health check for http(s) and rts(p)s. */
    public CompletableFuture<Boolean> checkAsync(URI uri) {
        if (uri == null) {
            log.debug("❌ URL is null → unavailable");
            return CompletableFuture.completedFuture(false);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();

        if (scheme.equals("http") || scheme.equals("https")) {
            return checkHttpAsync(uri);
        } else if (scheme.equals("rtsp") || scheme.equals("rtsps")) {
            return checkRtspAsync(uri);
        } else {
            log.debug("❌ [{}] → unsupported scheme '{}' → unavailable", uri, scheme);
            return CompletableFuture.completedFuture(false);
        }
    }

    /** Sync wrapper, if you still need a boolean directly. */
    public boolean isUrlReachable(URI uri) {
        try {
            return checkAsync(uri).orTimeout(hardTimeout.toMillis(), TimeUnit.MILLISECONDS).join();
        } catch (CompletionException e) {
            log.debug("❌ [{}] → exception/timeout: {} → unavailable", uri, e.toString());
            return false;
        }
    }

    // ---------- HTTP path (sendAsync) ----------

    private CompletableFuture<Boolean> checkHttpAsync(URI uri) {
        return sendAsyncStatus(uri, "GET")
                .thenCompose(code -> {
                    if (code == 405 || code >= 400) {
                        return sendAsyncStatus(uri, "HEAD");
                    }
                    return CompletableFuture.completedFuture(code);
                })
                .handle((code, err) -> {
                    if (err != null) {
                        String msg = (err instanceof CompletionException && err.getCause() != null)
                                ? err.getCause().toString() : err.toString();
                        log.debug("❌ [{}] → exception: {} → unavailable", uri, msg);
                        return false;
                    }
                    boolean ok = isReachableCode(code);
                    if (ok) log.debug("✅ [{}] → HTTP {} → available", uri, code);
                    else    log.debug("❌ [{}] → HTTP {} → unavailable", uri, code);
                    return ok;
                });
    }

    private CompletableFuture<Integer> sendAsyncStatus(URI uri, String method) {
        HttpRequest.Builder rb = HttpRequest.newBuilder(uri)
                .version(httpVersion)
                .timeout(hardTimeout)
                .header("User-Agent", "M3U-HealthCheck/1.5 (+Java 17)")
                .header("Accept", "application/x-mpegURL, application/vnd.apple.mpegurl, */*");

        HttpRequest req = "HEAD".equals(method)
                ? rb.method("HEAD", HttpRequest.BodyPublishers.noBody()).build()
                : rb.GET().build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                .orTimeout(hardTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(HttpResponse::statusCode);
    }

    // ---------- RTSP path (socket-based OPTIONS) ----------

    private CompletableFuture<Boolean> checkRtspAsync(URI uri) {
        return CompletableFuture.supplyAsync(() -> checkRtsp(uri), executor);
    }

    private boolean checkRtsp(URI uri) {
        String scheme = uri.getScheme().toLowerCase();
        String host = uri.getHost();
        if (host == null) {
            // For IPv6 literal or odd URIs, fallback parsing
            host = uri.getAuthority();
        }
        int port = uri.getPort();
        if (port < 0) {
            // Default ports: RTSP typically 554. Some servers use 8554; rtsps historically 322, often 554 too.
            port = 554;
        }

        // Build minimal RTSP/1.0 OPTIONS request
        String request =
                "OPTIONS " + uri.toString() + " RTSP/1.0\r\n" +
                        "CSeq: 1\r\n" +
                        "User-Agent: M3U-HealthCheck/1.5 (+Java 17)\r\n" +
                        "\r\n";

        int code = -1;
        try {
            // Connect
            Socket socket = RtspSocketFactory.of(scheme, host, port);
            socket.setSoTimeout((int) hardTimeout.toMillis());
            try (socket;
                 OutputStream out = socket.getOutputStream();
                 InputStream in = socket.getInputStream();
                 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
                 BufferedReader br = new BufferedReader(new InputStreamReader(in))) {

                // Send
                bw.write(request);
                bw.flush();

                // Read status line: RTSP/1.0 200 OK
                String statusLine = br.readLine();
                if (statusLine == null) {
                    log.debug("❌ [{}] → no response → unavailable", uri);
                    return false;
                }

                code = parseRtspStatus(statusLine);
                boolean ok = isReachableCode(code);

                if (ok) log.debug("✅ [{}] → RTSP {} → available", uri, code);
                else     log.debug("❌ [{}] → RTSP {} → unavailable", uri, code);

                // We don’t need to read headers/body further for a health check
                return ok;
            }
        } catch (Exception e) {
            log.debug("❌ [{}] → RTSP exception: {} → unavailable", uri, e.toString());
            return false;
        }
    }

    private static int parseRtspStatus(String statusLine) {
        // Example: RTSP/1.0 200 OK
        Pattern p = Pattern.compile("^RTSP/\\d\\.\\d\\s+(\\d{3})\\b");
        Matcher m = p.matcher(statusLine);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        return -1;
    }


}
