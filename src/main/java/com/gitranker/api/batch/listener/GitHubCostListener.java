package com.gitranker.api.batch.listener;

import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubCostListener implements JobExecutionListener {

    private final UserRepository userRepository;
    private final BatchProgressListener progressListener;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        progressListener.reset();

        int totalUserCount = (int) userRepository.count();

        log.debug("배치 작업 시작 - Job: {}, 총 사용자 수: {}",
                jobExecution.getJobInstance().getJobName(), totalUserCount);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        long durationMs = calculateDuration(jobExecution);

        BatchStatistics stats = aggregateStepStatistics(jobExecution);

        LogContext.event(Event.BATCH_COMPLETED)
                .with("job_name", jobName)
                .with("status", jobExecution.getStatus().toString())
                .with("total_count", stats.totalCount)
                .with("success_count", stats.successCount)
                .with("fail_count", stats.failCount)
                .with("skip_count", stats.skipCount)
                .with("duration_ms", durationMs)
                .info();
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
