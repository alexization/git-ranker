package com.gitranker.api.batch.scheduler;

import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job dailyScoreRecalculationJob;

    @Scheduled(cron = "0 0 0 * * *", zone = "${app.timezone}")
    public void runDailyScoreRecalculationJob() {
        final String jobName = "DailyScoreRecalculation";

        MdcUtils.setupBatchJobContext(jobName);
        MdcUtils.setEventType(EventType.REQUEST);

        log.debug("배치 Job 시작 - Name: {}", jobName);

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runTime", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(dailyScoreRecalculationJob, jobParameters);

            MdcUtils.setEventType(EventType.SUCCESS);
            log.info("배치 Job 완료 - Name: {}", jobName);

        } catch (Exception e) {
            MdcUtils.setLogContext(LogCategory.BATCH, EventType.FAILURE);
            MdcUtils.setError(ErrorType.BATCH_JOB_FAILED.name(), e.getMessage());

            log.error("배치 Job 실패 - Name: {}, Reason: {}", jobName, e.getMessage(), e);

            throw new BusinessException(ErrorType.BATCH_JOB_FAILED, e.getMessage());
        } finally {
            MdcUtils.clear();
        }
    }
}
