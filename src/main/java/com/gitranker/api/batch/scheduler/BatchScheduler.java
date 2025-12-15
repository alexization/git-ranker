package com.gitranker.api.batch.scheduler;

import com.gitranker.api.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.UUID;

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
        String traceId = "BATCH-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("trace_id", traceId);
        MDC.put("job_name", "DailyScoreRecalculation");
        MDC.put("client_ip", "SYSTEM");

        long start = System.currentTimeMillis();
        log.info("Daily 배치 작업 시작");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runTime", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(dailyScoreRecalculationJob, jobParameters);

            long end = System.currentTimeMillis();
            MDC.put("latency_ms", String.valueOf(end - start));
            log.info("Daily 배치 작업 종료");

        } catch (Exception e) {
            long end = System.currentTimeMillis();
            MDC.put("latency_ms", String.valueOf(end - start));
            log.error("Batch Job Failed: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void runHourlyRankingRecalculation() {
        String traceId = "BATCH-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("trace_id", traceId);
        MDC.put("job_name", "HourlyRankingRecalculation");
        MDC.put("client_ip", "SYSTEM");

        long start = System.currentTimeMillis();

        try {

            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long newUserCount = userRepository.countByCreatedAtAfter(oneHourAgo);

            if (newUserCount == 0) {
                log.info("Hourly 배치 작업 패스");
                return;
            }

            log.info("Hourly 배치 작업 시작");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("runTime", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(hourlyRankingJob, jobParameters);

            long end = System.currentTimeMillis();
            MDC.put("latency_ms", String.valueOf(end - start));
            log.info("Hourly 배치 작업 종료");

        } catch (Exception e) {
            long end = System.currentTimeMillis();
            MDC.put("latency_ms", String.valueOf(end - start));
            log.error("Batch Job Failed: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
}
