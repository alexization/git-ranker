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
    private int issueCount = 0;

    @Column(nullable = false)
    private int reviewCount = 0;

    @Column(nullable = false)
    private int dailyScore = 0;

    @Builder
    public ActivityLog(User user, LocalDate activityDate, int commitCount, int prCount, int issueCount, int reviewCount, int dailyScore) {
        this.user = user;
        this.activityDate = activityDate;
        this.commitCount = commitCount;
        this.prCount = prCount;
        this.issueCount = issueCount;
        this.reviewCount = reviewCount;
        this.dailyScore = dailyScore;
    }
}
