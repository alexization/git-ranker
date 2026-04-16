package com.gitranker.api.batch.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BatchMetricsTest {

    @Test
    @DisplayName("successful job recording increments success counter and timer")
    void recordsCompletedJob() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BatchMetrics metrics = new BatchMetrics(registry);

        metrics.recordJobCompleted(1250);

        assertThat(registry.get("batch_jobs_completed_total").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("batch_job_duration").tag("status", "success").timer().count()).isEqualTo(1L);
        assertThat(registry.get("batch_job_duration").tag("status", "success").timer().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                .isEqualTo(1250.0);
    }

    @Test
    @DisplayName("failed job recording increments failure counter and timer")
    void recordsFailedJob() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BatchMetrics metrics = new BatchMetrics(registry);

        metrics.recordJobFailed(500);

        assertThat(registry.get("batch_jobs_failed_total").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("batch_job_duration").tag("status", "failure").timer().count()).isEqualTo(1L);
        assertThat(registry.get("batch_job_duration").tag("status", "failure").timer().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                .isEqualTo(500.0);
    }

    @Test
    @DisplayName("item counters accumulate processed and skipped counts")
    void recordsProcessedAndSkippedItems() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BatchMetrics metrics = new BatchMetrics(registry);

        metrics.recordItemsProcessed(12);
        metrics.recordItemsSkipped(3);

        assertThat(registry.get("batch_items_processed_total").counter().count()).isEqualTo(12.0);
        assertThat(registry.get("batch_items_skipped_total").counter().count()).isEqualTo(3.0);
    }
}
