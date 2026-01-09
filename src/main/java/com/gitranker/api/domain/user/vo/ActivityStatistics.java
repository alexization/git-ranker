package com.gitranker.api.domain.user.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class ActivityStatistics {

    private final int commitCount;
    private final int issueCount;
    private final int prOpenedCount;
    private final int prMergedCount;
    private final int reviewCount;

    private ActivityStatistics(int commitCount, int issueCount, int prOpenedCount, int prMergedCount, int reviewCount) {
        this.commitCount = commitCount;
        this.issueCount = issueCount;
        this.prOpenedCount = prOpenedCount;
        this.prMergedCount = prMergedCount;
        this.reviewCount = reviewCount;
    }

    public static ActivityStatistics of(int commitCount, int issueCount, int prOpenedCount, int prMergedCount, int reviewCount) {
        return new ActivityStatistics(commitCount, issueCount, prOpenedCount, prMergedCount, reviewCount);
    }

    public static ActivityStatistics empty() {
        return new ActivityStatistics(0, 0, 0, 0, 0);
    }

    public static ActivityStatistics zeroDiff() {
        return new ActivityStatistics(0, 0, 0, 0, 0);
    }

    public Score calculateScore() {
        return Score.calculate(commitCount, issueCount, reviewCount, prOpenedCount, prMergedCount);
    }

    public ActivityStatistics calculateDiff(ActivityStatistics previous) {
        return new ActivityStatistics(
                this.commitCount - previous.commitCount,
                this.issueCount - previous.issueCount,
                this.prOpenedCount - previous.prOpenedCount,
                this.prMergedCount - previous.prMergedCount,
                this.reviewCount - previous.reviewCount
        );
    }

    public ActivityStatistics merge(ActivityStatistics other) {
        return new ActivityStatistics(
                this.commitCount + other.commitCount,
                this.issueCount + other.issueCount,
                this.prOpenedCount + other.prOpenedCount,
                this.prMergedCount + other.prMergedCount,
                this.reviewCount + other.reviewCount
        );
    }

    public boolean hasActivity() {
        return commitCount > 0 || issueCount > 0 || prOpenedCount > 0 || prMergedCount > 0 || reviewCount > 0;
    }

    public int totalActivityCount() {
        return commitCount + issueCount + prOpenedCount + prMergedCount + reviewCount;
    }

    @Override
    public String toString() {
        return String.format("ActivityStatistics{commits=%d, issues=%d, prs=%d, merged=%d, reviews=%d}",
                commitCount, issueCount, prOpenedCount, prMergedCount, reviewCount);
    }
}
