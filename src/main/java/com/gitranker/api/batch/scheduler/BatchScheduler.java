package com.gitranker.api.batch.scheduler;

import com.gitranker.api.domain.user.UserRepository;
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
        long start = System.currentTimeMillis();
        log.info("[Batch Start] DailyScoreRecalculation");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runTime", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(dailyScoreRecalculationJob, jobParameters);

            long end = System.currentTimeMillis();
            log.info("[Batch End] DailyScoreRecalculation | Latency: {}ms", end - start);
        } catch (Exception e) {
            log.error("[Batch Failed] DailyScoreRecalculation | Msg: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void runHourlyRankingRecalculation() {
        long start = System.currentTimeMillis();

        try {

            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long newUserCount = userRepository.countByCreatedAtAfter(oneHourAgo);

            if (newUserCount == 0) {
                log.info("[Batch Skip] HourlyRankingRecalculation | No new users");
                return;
            }

            log.info("[Batch Start] HourlyRankingRecalculation");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runTime", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(hourlyRankingJob, jobParameters);

            long end = System.currentTimeMillis();
            log.info("[Batch End] HourlyRankingRecalculation | Latency: {}ms", end - start);
        } catch (Exception e) {
            log.error("[Batch Failed] HourlyRankingRecalculation | Msg: {}", e.getMessage());
        }
    }
}
