package com.gitranker.api.infrastructure.github.dto;


import com.gitranker.api.domain.user.vo.ActivityStatistics;

public record GitHubActivitySummary(
        int totalCommitCount,
        int totalPrOpenedCount,
        int totalPrMergedCount,
        int totalIssueCount,
        int totalReviewCount
) {
    public ActivityStatistics toActivityStatistics() {
        return ActivityStatistics.of(
                totalCommitCount,
                totalIssueCount,
                totalPrOpenedCount,
                totalPrMergedCount,
                totalReviewCount
        );
    }
}
