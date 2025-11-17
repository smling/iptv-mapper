package io.github.smling.iptv_mapper.perf;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class M3uEpgPerfBenchmark {

    // Simple placeholder benchmark to validate JMH wiring.
    // Extend this with real endpoints / service-level benchmarking as needed.

    private String sample = "channel-id-123";

    @Benchmark
    public String simpleStringConcat() {
        // Mimic light work similar to building M3U/EPG identifiers
        return "tvg-id=" + sample;
    }
}

