package com.gitranker.api.batch.strategy;

import com.gitranker.api.domain.log.ActivityLog;

public record ActivityUpdateContext(
        ActivityLog baselineLog,
        int currentYear
) {
    public static ActivityUpdateContext forIncremental(ActivityLog baselineLog, int currentYear) {
        if (baselineLog == null) {
            throw new IllegalArgumentException("증분 업데이트에는 baselineLog가 필요합니다.");
        }

        return new ActivityUpdateContext(baselineLog, currentYear);
    }

    public static ActivityUpdateContext forFull(int currentYear) {
        return new ActivityUpdateContext(null, currentYear);
    }

    public boolean hasBaselineLog() {
        return baselineLog != null;
    }
}
