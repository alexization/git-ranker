package com.gitranker.api.infrastructure.github.dto;


public record GitHubActivitySummary(
        int totalCommitCount,
        int totalPrOpenedCount,
        int totalPrMergedCount,
        int totalIssueCount,
        int totalReviewCount
) {
    public int calculateTotalScore() {
        return (totalCommitCount)
                + (totalIssueCount * 2)
                + (totalReviewCount * 3)
                + (totalPrOpenedCount * 5)
                + (totalPrMergedCount * 10);
    }
}
