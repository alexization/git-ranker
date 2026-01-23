package com.gitranker.api.infrastructure.github;

import com.gitranker.api.infrastructure.github.token.TokenState;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class GitHubApiMetrics {

    private static final String METRIC_PREFIX = "github_api";

    private final AtomicInteger remaining = new AtomicInteger(TokenState.DEFAULT_LIMIT);
    private final AtomicLong resetAtEpoch = new AtomicLong(0);
    private final AtomicReference<String> resetAtFormatted = new AtomicReference<>("N/A");

    private final Counter costCounter;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter rateLimitCounter;

    private final Timer latencyTimer;

    public GitHubApiMetrics(MeterRegistry registry) {
        Gauge.builder(METRIC_PREFIX + "_remaining", remaining, AtomicInteger::get)
                .description("GitHub API Rate Limit remaining calls")
                .register(registry);

        Gauge.builder(METRIC_PREFIX + "_reset_at_epoch", resetAtEpoch, AtomicLong::get)
                .description("GitHub API Rate Limit reset time (Unix epoch seconds)")
                .register(registry);

        costCounter = Counter.builder(METRIC_PREFIX + "_cost_total")
                .description("Total GitHub GraphQL query cost")
                .register(registry);

        successCounter = Counter.builder(METRIC_PREFIX + "_calls_total")
                .tag("result", "success")
                .description("Total GitHub API calls")
                .register(registry);

        failureCounter = Counter.builder(METRIC_PREFIX + "_calls_total")
                .tag("result", "failure")
                .description("Total GitHub API calls")
                .register(registry);

        rateLimitCounter = Counter.builder(METRIC_PREFIX + "_calls_total")
                .tag("result", "rate_limited")
                .description("Total GitHub API calls")
                .register(registry);

        latencyTimer = Timer.builder(METRIC_PREFIX + "_latency")
                .description("GitHub API call latency")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(registry);
    }

    public void recordRateLimit(int cost, int remaining, LocalDateTime resetAt) {
        this.remaining.set(remaining);

        if (resetAt != null) {
            this.resetAtEpoch.set(resetAt.atZone(java.time.ZoneId.systemDefault()).toEpochSecond());
            this.resetAtFormatted.set(resetAt.toString());
        }

        if (cost > 0) {
            costCounter.increment(cost);
        }
    }

    public void recordSuccess(long latencyMs) {
        successCounter.increment();
        latencyTimer.record(java.time.Duration.ofMillis(latencyMs));
    }

    public void recordFailure() {
        failureCounter.increment();
    }

    public void recordRateLimitExceeded() {
        rateLimitCounter.increment();
    }

    public int getRemaining() {
        return remaining.get();
    }

    public String getResetAtFormatted() {
        return resetAtFormatted.get();
    }
}
