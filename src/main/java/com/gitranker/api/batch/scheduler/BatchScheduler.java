package com.gitranker.api.batch.scheduler;

import com.gitranker.api.domain.user.UserRepository;
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
    public void runDailyScoreRecalculationJob() {
        MdcUtils.setupBatchJobContext("DailyScoreRecalculation");
        log.info("Daily 배치 작업 시작");

        long start = System.currentTimeMillis();

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runTime", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(dailyScoreRecalculationJob, jobParameters);

            long latency = System.currentTimeMillis() - start;
            MdcUtils.setLatency(latency);
            log.info("Daily 배치 작업 완료");

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            MdcUtils.setLatency(latency);
            log.error("Daily 배치 작업 실패: {}", e.getMessage(), e);
        } finally {
            MdcUtils.clear();
        }
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void runHourlyRankingRecalculation() {
        MdcUtils.setupBatchJobContext("HourlyRankingRecalculation");

        long start = System.currentTimeMillis();

        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long newUserCount = userRepository.countByCreatedAtAfter(oneHourAgo);

            if (newUserCount == 0) {
                log.info("배치 작업 패스");
                return;
            }

            log.info("Hourly 배치 작업 시작");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runTime", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(hourlyRankingJob, jobParameters);

            long latency = System.currentTimeMillis() - start;
            MdcUtils.setLatency(latency);
            log.info("Hourly 배치 작업 종료");

        } catch (Exception e) {
            long latency = System.currentTimeMillis();
            MdcUtils.setLatency(latency);
            log.error("Hourly 배치 실패: {}", e.getMessage(), e);
        } finally {
            MdcUtils.clear();
        }
    }
}
