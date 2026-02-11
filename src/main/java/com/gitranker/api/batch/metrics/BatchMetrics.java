package com.gitranker.api.batch.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class BatchMetrics {

    private final Counter jobsCompleted;
    private final Counter jobsFailed;
    private final Counter itemsProcessed;
    private final Counter itemsSkipped;
    private final Timer jobDuration;

    public BatchMetrics(MeterRegistry registry) {
        jobsCompleted = Counter.builder("batch_jobs_completed_total")
                .description("Total batch jobs completed successfully")
                .register(registry);

        jobsFailed = Counter.builder("batch_jobs_failed_total")
                .description("Total batch jobs failed")
                .register(registry);

        itemsProcessed = Counter.builder("batch_items_processed_total")
                .description("Total batch items processed")
                .register(registry);

        itemsSkipped = Counter.builder("batch_items_skipped_total")
                .description("Total batch items skipped")
                .register(registry);

        jobDuration = Timer.builder("batch_job_duration_seconds")
                .description("Batch job execution duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void recordJobCompleted(long durationMs) {
        jobsCompleted.increment();
        jobDuration.record(Duration.ofMillis(durationMs));
    }

    public void recordJobFailed(long durationMs) {
        jobsFailed.increment();
        jobDuration.record(Duration.ofMillis(durationMs));
    }

    public void recordItemsProcessed(long count) {
        itemsProcessed.increment(count);
    }

    public void recordItemsSkipped(long count) {
        itemsSkipped.increment(count);
    }
}
