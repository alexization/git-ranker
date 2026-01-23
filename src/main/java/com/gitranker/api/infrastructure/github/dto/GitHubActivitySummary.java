package com.gitranker.api.infrastructure.github.dto;


import com.gitranker.api.domain.user.vo.ActivityStatistics;

public record GitHubActivitySummary(
        int commitCount,
        int prOpenedCount,
        int prMergedCount,
        int issueCount,
        int reviewCount
) {
    public ActivityStatistics toActivityStatistics() {
        return ActivityStatistics.of(
                commitCount,
                issueCount,
                prOpenedCount,
                prMergedCount,
                reviewCount
        );
    }
}
