package io.github.smling.iptv_mapper.factories;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HttpClientFactory {
    // bump to a fixed 32-thread pool and bigger queue
    private static final int CORE_POOL_SIZE = 12;
    private static final int MAX_POOL_SIZE  = 12;
    private static final int QUEUE_CAPACITY = 200; // was 200

    private static final ExecutorService threadPoolExecutor = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public static HttpClient of() {
        return HttpClient.newBuilder()
                .executor(threadPoolExecutor)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_2)
                .build();
    }
}
