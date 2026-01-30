package com.gitranker.api.domain.log;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "activity_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate activityDate;

    @Column(nullable = false)
    private int commitCount = 0;

    @Column(nullable = false)
    private int prCount = 0;

    @Column(nullable = false)
    private int mergedPrCount = 0;

    @Column(nullable = false)
    private int issueCount = 0;

    @Column(nullable = false)
    private int reviewCount = 0;

    @Column(nullable = false)
    private int diffCommitCount = 0;

    @Column(nullable = false)
    private int diffPrCount = 0;

    @Column(nullable = false)
    private int diffMergedPrCount = 0;

    @Column(nullable = false)
    private int diffIssueCount = 0;

    @Column(nullable = false)
    private int diffReviewCount = 0;

    @Builder
    public ActivityLog(User user, LocalDate activityDate, int commitCount, int prCount, int mergedPrCount, int issueCount, int reviewCount, int diffCommitCount, int diffPrCount, int diffMergedPrCount, int diffIssueCount, int diffReviewCount) {
        this.user = user;
        this.activityDate = activityDate;
        this.commitCount = commitCount;
        this.prCount = prCount;
        this.mergedPrCount = mergedPrCount;
        this.issueCount = issueCount;
        this.reviewCount = reviewCount;
        this.diffCommitCount = diffCommitCount;
        this.diffPrCount = diffPrCount;
        this.diffMergedPrCount = diffMergedPrCount;
        this.diffIssueCount = diffIssueCount;
        this.diffReviewCount = diffReviewCount;
    }

    public static ActivityLog of(User user, ActivityStatistics stats, ActivityStatistics diff, LocalDate date) {
        return ActivityLog.builder()
                .user(user)
                .activityDate(date)
                .commitCount(stats.getCommitCount())
                .issueCount(stats.getIssueCount())
                .prCount(stats.getPrOpenedCount())
                .mergedPrCount(stats.getPrMergedCount())
                .reviewCount(stats.getReviewCount())
                .diffCommitCount(diff.getCommitCount())
                .diffIssueCount(diff.getIssueCount())
                .diffPrCount(diff.getPrOpenedCount())
                .diffMergedPrCount(diff.getPrMergedCount())
                .diffReviewCount(diff.getReviewCount())
                .build();
    }

    public static ActivityLog baseline(User user, ActivityStatistics stats, LocalDate date) {
        return ActivityLog.builder()
                .user(user)
                .activityDate(date)
                .commitCount(stats.getCommitCount())
                .issueCount(stats.getIssueCount())
                .prCount(stats.getPrOpenedCount())
                .mergedPrCount(stats.getPrMergedCount())
                .reviewCount(stats.getReviewCount())
                .diffCommitCount(0)
                .diffIssueCount(0)
                .diffPrCount(0)
                .diffMergedPrCount(0)
                .diffReviewCount(0)
                .build();
    }

    public static ActivityLog empty(User user, LocalDate date) {
        return ActivityLog.builder()
                .user(user)
                .activityDate(date)
                .commitCount(0).issueCount(0).prCount(0).mergedPrCount(0).reviewCount(0)
                .diffCommitCount(0).diffIssueCount(0).diffPrCount(0)
                .diffMergedPrCount(0).diffReviewCount(0)
                .build();
    }

    public ActivityStatistics toStatistics() {
        return ActivityStatistics.of(
                this.commitCount,
                this.issueCount,
                this.prCount,
                this.mergedPrCount,
                this.reviewCount
        );
    }

    public void updateStatistics(ActivityStatistics stats) {
        this.commitCount = stats.getCommitCount();
        this.issueCount = stats.getIssueCount();
        this.prCount = stats.getPrOpenedCount();
        this.mergedPrCount = stats.getPrMergedCount();
        this.reviewCount = stats.getReviewCount();
    }

    public void updateStatisticsWithDiff(ActivityStatistics stats, ActivityStatistics diff) {
        this.commitCount = stats.getCommitCount();
        this.issueCount = stats.getIssueCount();
        this.prCount = stats.getPrOpenedCount();
        this.mergedPrCount = stats.getPrMergedCount();
        this.reviewCount = stats.getReviewCount();
        this.diffCommitCount = diff.getCommitCount();
        this.diffIssueCount = diff.getIssueCount();
        this.diffPrCount = diff.getPrOpenedCount();
        this.diffMergedPrCount = diff.getPrMergedCount();
        this.diffReviewCount = diff.getReviewCount();
    }
}
