package com.gitranker.api.domain.log;

import com.gitranker.api.domain.user.User;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDate activityDate;

    private int commitCount;
    private int prCount;
    private int issueCount;
    private int reviewCount;

    private int dailyScore;
}
