package com.gitranker.api.domain.log;

import com.gitranker.api.domain.user.User;
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
}
