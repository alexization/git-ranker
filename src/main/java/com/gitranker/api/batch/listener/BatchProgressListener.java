package com.gitranker.api.batch.listener;

import com.gitranker.api.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchProgressListener implements ChunkListener {

    private static final int PROGRESS_LOG_INTERVAL = 10;

    private final UserRepository userRepository;

    private int totalCount = 0;
    private int lastLoggedPercentage = 0;

    @Override
    public void beforeChunk(ChunkContext context) {
        if (totalCount == 0) {
            totalCount = (int) userRepository.count();
            lastLoggedPercentage = 0;
        }
    }

    @Override
    public void afterChunk(ChunkContext context) {
        if (totalCount == 0) {
            return;
        }

        String jobName = context.getStepContext().getJobName();
        long processedCount = context.getStepContext().getStepExecution().getWriteCount();

        int currentPercentage = (int) ((processedCount * 100) / totalCount);

        if (currentPercentage >= lastLoggedPercentage + PROGRESS_LOG_INTERVAL) {
            int roundedPercentage = (currentPercentage / PROGRESS_LOG_INTERVAL) * PROGRESS_LOG_INTERVAL;

            log.debug("배치 진행 - Job: {}, 진행: {}/{} ({}%)",
                    jobName, processedCount, totalCount, roundedPercentage);

            lastLoggedPercentage = roundedPercentage;
        }
    }

    @Override
    public void afterChunkError(ChunkContext context) {
    }

    public void reset() {
        totalCount = 0;
        lastLoggedPercentage = 0;
    }
}
