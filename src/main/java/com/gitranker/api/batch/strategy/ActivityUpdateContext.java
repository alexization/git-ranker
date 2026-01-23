package com.gitranker.api.batch.strategy;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.global.error.message.BatchMessages;

public record ActivityUpdateContext(
        ActivityLog baselineLog,
        int currentYear
) {
    public static ActivityUpdateContext forIncremental(ActivityLog baselineLog, int currentYear) {
        if (baselineLog == null) {
            throw new IllegalStateException(BatchMessages.BASELINE_LOG_REQUIRED_FOR_INCREMENTAL);
        }

        return new ActivityUpdateContext(baselineLog, currentYear);
    }

    public static ActivityUpdateContext forFull(int currentYear) {
        return new ActivityUpdateContext(null, currentYear);
    }
}
