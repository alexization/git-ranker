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
        try {
            log.info("===== 점수 재계산 배치 시작 =====");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runTime", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(dailyScoreRecalculationJob, jobParameters);

            log.info("===== 점수 재계산 배치 완료 =====");
        } catch (Exception e) {
            log.error("배치 실행 실패 : {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void runHourlyRankingRecalculation() {
        try {
            log.info("===== Hourly 순위 재조정 배치 시작 =====");

            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long newUserCount = userRepository.countByCreatedAtAfter(oneHourAgo);

            if (newUserCount == 0) {
                return;
            }

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runTime", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(hourlyRankingJob, jobParameters);

            log.info("===== Hourly 순위 재조정 배치 완료 =====");
        } catch (Exception e) {
            log.error("Hourly 배치 실행 실패", e);
        }
    }
}
