package com.gitranker.api.batch.listener;

import com.gitranker.api.batch.metrics.BatchMetrics;
import com.gitranker.api.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import java.time.LocalDateTime;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubCostListenerTest {

    @InjectMocks
    private GitHubCostListener listener;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BatchProgressListener progressListener;

    @Mock
    private BatchMetrics batchMetrics;

    @Test
    @DisplayName("beforeJob resets progress listener and counts users")
    void resetsProgressAndCountsUsersBeforeJob() {
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, "DailyScoreRecalculationJob"), new JobParameters());
        when(userRepository.count()).thenReturn(150L);

        listener.beforeJob(jobExecution);

        verify(progressListener).reset();
        verify(userRepository).count();
    }

    @Test
    @DisplayName("afterJob records completed metrics from score recalculation step")
    void recordsSuccessMetrics() {
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, "DailyScoreRecalculationJob"), new JobParameters());
        StepExecution stepExecution = jobExecution.createStepExecution("scoreRecalculationStep");
        stepExecution.setReadCount(100);
        stepExecution.setWriteCount(92);
        stepExecution.setProcessSkipCount(3);
        stepExecution.setWriteSkipCount(2);
        stepExecution.setFilterCount(5);
        jobExecution.setStatus(BatchStatus.COMPLETED);
        jobExecution.setStartTime(LocalDateTime.of(2026, 4, 16, 10, 0, 0));
        jobExecution.setEndTime(LocalDateTime.of(2026, 4, 16, 10, 0, 2));

        listener.afterJob(jobExecution);

        verify(batchMetrics).recordJobCompleted(2000L);
        verify(batchMetrics, never()).recordJobFailed(org.mockito.ArgumentMatchers.anyLong());
        verify(batchMetrics).recordItemsProcessed(92);
        verify(batchMetrics).recordItemsSkipped(5);
    }

    @Test
    @DisplayName("afterJob records failed metrics and ignores non-score steps")
    void recordsFailureMetricsAndIgnoresOtherSteps() {
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, "DailyScoreRecalculationJob"), new JobParameters());
        StepExecution stepExecution = jobExecution.createStepExecution("cleanupStep");
        stepExecution.setReadCount(20);
        stepExecution.setWriteCount(18);
        stepExecution.setFilterCount(1);
        jobExecution.setStatus(BatchStatus.FAILED);

        listener.afterJob(jobExecution);

        verify(batchMetrics).recordJobFailed(0L);
        verify(batchMetrics, never()).recordJobCompleted(org.mockito.ArgumentMatchers.anyLong());
        verify(batchMetrics).recordItemsProcessed(0);
        verify(batchMetrics).recordItemsSkipped(0);
    }
}
