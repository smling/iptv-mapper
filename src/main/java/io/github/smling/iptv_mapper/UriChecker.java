package io.github.smling.iptv_mapper;

import io.github.smling.iptv_mapper.factories.RtspSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UriChecker {

    public static final class UrlCheckResult {
        private final Reachability status;
        private final long responseTimeMillis;

        public UrlCheckResult(Reachability status, long responseTimeMillis) {
            this.status = status;
            this.responseTimeMillis = responseTimeMillis;
        }
        public Reachability status() { return status; }
        public long responseTimeMillis() { return responseTimeMillis; }
        public boolean isReachable() { return status == Reachability.Reachable; }

        @Override public String toString() {
            return "UrlCheckResult{" + status + ", " + responseTimeMillis + "ms}";
        }
    }

    private static final Logger log = LoggerFactory.getLogger(UriChecker.class);

    private final HttpClient http;
    private final Duration hardTimeout;
    private final HttpClient.Version httpVersion;
    private final Executor executor;   // for RTSP socket work

    // In new semantics, only 200 is considered Reachable; other codes are Inaccessable
    private static boolean isHttpOk200(int code) { return code == 200; }

    public UriChecker(HttpClient http, Duration hardTimeout, HttpClient.Version httpVersion, Executor executor) {
        this.http = Objects.requireNonNull(http);
        this.hardTimeout = hardTimeout == null ? Duration.ofSeconds(7) : hardTimeout;
        this.httpVersion = httpVersion == null ? HttpClient.Version.HTTP_1_1 : httpVersion;
        this.executor = executor != null ? executor : ForkJoinPool.commonPool();
    }

    public UriChecker() {
        this(buildLowPriorityHttpClient(),
                Duration.ofSeconds(7),
                HttpClient.Version.HTTP_1_1,
                LOW_PRIORITY_EXECUTOR);
    }

    // Low-priority thread setup to de-prioritize health checks vs. normal traffic
    private static final ExecutorService LOW_PRIORITY_EXECUTOR = new ThreadPoolExecutor(
            2, 4,
            30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(128),
            r -> {
                Thread t = new Thread(r);
                t.setName("uri-checker-lp-" + t.threadId());
                t.setDaemon(true);
                t.setPriority(Math.max(Thread.NORM_PRIORITY - 2, Thread.MIN_PRIORITY));
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private static HttpClient buildLowPriorityHttpClient() {
        return HttpClient.newBuilder()
                .executor(LOW_PRIORITY_EXECUTOR)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    /** Async health check for http(s)/rts(p)s returning status and response time. */
    public CompletableFuture<UrlCheckResult> checkAsync(URI uri) {
        if (uri == null) {
            log.debug("❌ URL is null → unreachable");
            return CompletableFuture.completedFuture(new UrlCheckResult(Reachability.Unreachable, 0));
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();

        if (scheme.equals("http") || scheme.equals("https")) {
            return checkHttpAsync(uri);
        } else if (scheme.equals("rtsp") || scheme.equals("rtsps")) {
            return checkRtspAsync(uri);
        } else {
            log.debug("❌ [{}] → unsupported scheme '{}' → unreachable", uri, scheme);
            return CompletableFuture.completedFuture(new UrlCheckResult(Reachability.Unreachable, 0));
        }
    }

    /** Sync wrapper returning result with timing. */
    public UrlCheckResult check(URI uri) {
        try {
            return checkAsync(uri).orTimeout(hardTimeout.toMillis(), TimeUnit.MILLISECONDS).join();
        } catch (CompletionException e) {
            log.debug("❌ [{}] → exception/timeout: {} → unreachable", uri, e.toString());
            return new UrlCheckResult(Reachability.Unreachable, hardTimeout.toMillis());
        }
    }

    /** Legacy boolean method: true only for Reachable. */
    public boolean isUrlReachable(URI uri) {
        return check(uri).isReachable();
    }

    // ---------- HTTP path (sendAsync) ----------

    private CompletableFuture<UrlCheckResult> checkHttpAsync(URI uri) {
        final long startNanos = System.nanoTime();
        // Prefer HEAD first to minimize payload; fallback to GET only if HEAD unsupported
        return sendAsyncStatus(uri, "HEAD")
                .thenCompose(headCode -> {
                    if (isHttpOk200(headCode)) {
                        log.debug("✅ [{}] → HTTP-HEAD {} → reachable", uri, headCode);
                        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                        return CompletableFuture.completedFuture(new UrlCheckResult(Reachability.Reachable, ms));
                    }
                    if (headCode == 405 || headCode == 501) {
                        return sendAsyncStatus(uri, "GET")
                                .handle((getCode, getErr) -> {
                                    long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                                    if (getErr != null) {
                                        String msg = (getErr instanceof CompletionException && getErr.getCause() != null)
                                                ? getErr.getCause().toString() : getErr.toString();
                                        log.debug("❌ [{}] → GET exception: {} → unreachable", uri, msg);
                                        return new UrlCheckResult(Reachability.Unreachable, ms);
                                    }
                                    if (isHttpOk200(getCode)) {
                                        log.debug("✅ [{}] → HTTP-GET {} → reachable", uri, getCode);
                                        return new UrlCheckResult(Reachability.Reachable, ms);
                                    } else {
                                        log.debug("❌ [{}] → HTTP-GET {} → inaccessable", uri, getCode);
                                        return new UrlCheckResult(Reachability.Inaccessable, ms);
                                    }
                                });
                    }
                    log.debug("❌ [{}] → HTTP-HEAD {} → inaccessable", uri, headCode);
                    long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                    return CompletableFuture.completedFuture(new UrlCheckResult(Reachability.Inaccessable, ms));
                })
                .exceptionally(err -> {
                    long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                    String msg = (err.getCause() != null) ? err.getCause().toString() : err.toString();
                    log.debug("❌ [{}] → HEAD exception: {} → unreachable", uri, msg);
                    return new UrlCheckResult(Reachability.Unreachable, ms);
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

    private CompletableFuture<UrlCheckResult> checkRtspAsync(URI uri) {
        return CompletableFuture.supplyAsync(() -> checkRtsp(uri), executor);
    }

    private UrlCheckResult checkRtsp(URI uri) {
        final long startNanos = System.nanoTime();
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
                    log.debug("❌ [{}] → no response → unreachable", uri);
                    long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                    return new UrlCheckResult(Reachability.Unreachable, ms);
                }

                code = parseRtspStatus(statusLine);
                if (code == 200) {
                    log.debug("✅ [{}] → RTSP {} → reachable", uri, code);
                    long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                    return new UrlCheckResult(Reachability.Reachable, ms);
                } else {
                    log.debug("❌ [{}] → RTSP {} → inaccessable", uri, code);
                    long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                    return new UrlCheckResult(Reachability.Inaccessable, ms);
                }
            }
        } catch (Exception e) {
            log.debug("❌ [{}] → RTSP exception: {} → unreachable", uri, e.toString());
            long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            return new UrlCheckResult(Reachability.Unreachable, ms);
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
