package com.gitranker.api.domain.activity;

import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;

public class Activity {
    private int totalCommitCount = 0;
    private int totalPrOpenedCount = 0;
    private int totalPrMergedCount = 0;
    private int totalIssueCount = 0;
    private int totalReviewCount = 0;

    public static Activity builder() {
        return new Activity();
    }

    public Activity addCommits(int count) {
        this.totalCommitCount += count;
        return this;
    }

    public Activity addPrOpened(int count) {
        this.totalPrOpenedCount += count;
        return this;
    }

    public Activity addPrMerged(int count) {
        this.totalPrMergedCount += count;
        return this;
    }

    public Activity addIssues(int count) {
        this.totalIssueCount += count;
        return this;
    }

    public Activity addReviews(int count) {
        this.totalReviewCount += count;
        return this;
    }

    public GitHubActivitySummary build() {
        return new GitHubActivitySummary(
                totalCommitCount,
                totalPrOpenedCount,
                totalPrMergedCount,
                totalIssueCount,
                totalReviewCount
        );
    }
}
