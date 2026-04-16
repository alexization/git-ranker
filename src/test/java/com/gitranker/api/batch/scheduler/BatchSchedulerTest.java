package com.gitranker.api.batch.scheduler;

import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.logging.LogContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchSchedulerTest {

    @InjectMocks
    private BatchScheduler scheduler;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job dailyScoreRecalculationJob;

    @AfterEach
    void tearDown() {
        LogContext.clear();
    }

    @Test
    @DisplayName("scheduler launches the batch job with runtime parameter and clears log context")
    void launchesJob() throws Exception {
        when(jobLauncher.run(eq(dailyScoreRecalculationJob), any(JobParameters.class))).thenReturn(new JobExecution(1L));

        scheduler.runDailyScoreRecalculationJob();

        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(dailyScoreRecalculationJob), captor.capture());
        assertThat(captor.getValue().getLocalDateTime("runTime")).isNotNull();
        assertThat(LogContext.getTraceId()).isNull();
    }

    @Test
    @DisplayName("scheduler wraps launcher failure as batch job business exception and clears log context")
    void wrapsLauncherFailure() throws Exception {
        when(jobLauncher.run(eq(dailyScoreRecalculationJob), any(JobParameters.class)))
                .thenThrow(new IllegalStateException("job already running"));

        assertThatThrownBy(() -> scheduler.runDailyScoreRecalculationJob())
                .isInstanceOf(BusinessException.class)
                .satisfies(throwable -> {
                    BusinessException exception = (BusinessException) throwable;
                    assertThat(exception.getErrorType()).isEqualTo(ErrorType.BATCH_JOB_FAILED);
                    assertThat(exception.getData()).isEqualTo("job already running");
                });

        assertThat(LogContext.getTraceId()).isNull();
    }
}
