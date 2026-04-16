package com.gitranker.api.infrastructure.github;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubApiMetricsTest {

    @Test
    @DisplayName("recordRateLimit updates gauges and cost counter")
    void recordsRateLimitInformation() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        GitHubApiMetrics metrics = new GitHubApiMetrics(registry);
        LocalDateTime resetAt = LocalDateTime.of(2026, 4, 16, 18, 0);

        metrics.recordRateLimit(3, 120, resetAt);

        assertThat(metrics.getRemaining()).isEqualTo(120);
        assertThat(metrics.getResetAtFormatted()).isEqualTo("2026-04-16T18:00");
        assertThat(registry.get("github_api_cost_total").counter().count()).isEqualTo(3.0);
        assertThat(registry.get("github_api_remaining").gauge().value()).isEqualTo(120.0);
    }

    @Test
    @DisplayName("success failure and rate-limit counters are tracked independently")
    void recordsCallOutcomes() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        GitHubApiMetrics metrics = new GitHubApiMetrics(registry);

        metrics.recordSuccess(250);
        metrics.recordFailure();
        metrics.recordRateLimitExceeded();

        assertThat(registry.get("github_api_calls_total").tag("result", "success").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("github_api_calls_total").tag("result", "failure").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("github_api_calls_total").tag("result", "rate_limited").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("github_api_latency").timer().count()).isEqualTo(1L);
        assertThat(registry.get("github_api_latency").timer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(250.0);
    }
}
