package com.gitranker.api.batch.scheduler;

import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job dailyScoreRecalculationJob;

    @Scheduled(cron = "0 0 0 * * *", zone = "${app.timezone}")
    public void runDailyScoreRecalculationJob() {
        final String jobName = "DailyScoreRecalculation";

        LogContext.setTraceId(LogContext.generateTraceId());

        LogContext.event(Event.BATCH_STARTED)
                .with("job_name", jobName)
                .info();

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runTime", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(dailyScoreRecalculationJob, jobParameters);

        } catch (Exception e) {
            LogContext.event(Event.BATCH_ITEM_FAILED)
                    .with("job_name", jobName)
                    .with("error_message", e.getMessage())
                    .error();

            throw new BusinessException(ErrorType.BATCH_JOB_FAILED, e.getMessage());
        } finally {
            LogContext.clear();
        }
    }
}
