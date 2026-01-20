package com.gitranker.api.batch.listener;

import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.logging.BusinessEventLogger;
import com.gitranker.api.global.logging.MdcKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubCostListener implements JobExecutionListener {

    public static final String TOTAL_COST_KEY = "totalGitHubCost";

    private final UserRepository userRepository;
    private final BusinessEventLogger eventLogger;
    private final BatchProgressListener progressListener;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        jobExecution.getExecutionContext().putInt(TOTAL_COST_KEY, 0);

        progressListener.reset();

        int totalUserCount = (int) userRepository.count();

        eventLogger.batchStarted(jobExecution.getJobInstance().getJobName(), totalUserCount);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        int totalCost = jobExecution.getExecutionContext().getInt(TOTAL_COST_KEY, 0);
        long durationMs = calculateDuration(jobExecution);

        BatchStatistics stats = aggregateStepStatistics(jobExecution);

        MDC.put(MdcKey.GITHUB_API_COST, String.valueOf(totalCost));

        eventLogger.batchCompleted(
                jobName,
                stats.totalCount,
                stats.successCount,
                stats.failCount,
                stats.skipCount,
                durationMs
        );

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("배치 작업 GitHub API 총 비용: {}", totalCost);
        }
    }

    private long calculateDuration(JobExecution jobExecution) {
        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            return java.time.Duration.between(
                    jobExecution.getStartTime(),
                    jobExecution.getEndTime()
            ).toMillis();
        }
        return 0;
    }

    private BatchStatistics aggregateStepStatistics(JobExecution jobExecution) {
        long totalCount = 0;
        long successCount = 0;
        long failCount = 0;
        long skipCount = 0;

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if ("scoreRecalculationStep".equals(stepExecution.getStepName())) {
                totalCount = stepExecution.getReadCount();
                successCount = stepExecution.getWriteCount();
                failCount = stepExecution.getProcessSkipCount() + stepExecution.getWriteSkipCount();
                skipCount = stepExecution.getFilterCount();
            }
        }

        return new BatchStatistics(totalCount, successCount, failCount, skipCount);
    }

    private record BatchStatistics(long totalCount, long successCount, long failCount, long skipCount) {
    }
}
