package com.gitranker.api.batch.scheduler;

import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.aop.LogExecutionTime;
import com.gitranker.api.global.exception.BusinessException;
import com.gitranker.api.global.exception.ErrorType;
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
    private final UserRepository userRepository;
    private final Job dailyScoreRecalculationJob;
    private final Job hourlyRankingJob;

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @LogExecutionTime
    public void runDailyScoreRecalculationJob() {
        MdcUtils.setupBatchJobContext("DailyScoreRecalculation");
        log.info("[Batch Job] Job 시작 - Name: DailyScoreRecalculation");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runTime", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(dailyScoreRecalculationJob, jobParameters);

            log.info("[Batch Job] Job 완료 - Name: DailyScoreRecalculation");

        } catch (Exception e) {
            log.error("[Batch Job] Job 실패 - Name: DailyScoreRecalculation, Reason: {}", e.getMessage(), e);

            throw new BusinessException(ErrorType.BATCH_JOB_FAILED, e.getMessage());
        } finally {
            MdcUtils.clear();
        }
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    @LogExecutionTime
    public void runHourlyRankingRecalculation() {
        MdcUtils.setupBatchJobContext("HourlyRankingRecalculation");

        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long newUserCount = userRepository.countByCreatedAtAfter(oneHourAgo);

            if (newUserCount == 0) {
                log.info("[Batch Job] Job 건너뜀 - Name: HourlyRankingRecalculation");
                return;
            }

            log.info("[Batch Job] Job 시작 - Name: HourlyRankingRecalculation");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runTime", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(hourlyRankingJob, jobParameters);

            log.info("[Batch Job] Job 완료 - Name: HourlyRankingRecalculation");

        } catch (Exception e) {
            log.error("[Batch Error] Job 실패 - Name: HourlyRankingRecalculation, Reason: {}", e.getMessage(), e);

            throw new BusinessException(ErrorType.BATCH_JOB_FAILED, e.getMessage());
        } finally {
            MdcUtils.clear();
        }
    }
}
