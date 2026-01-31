package com.gitranker.api.domain.user;

import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.domain.user.vo.RankInfo;
import com.gitranker.api.domain.user.vo.Score;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_score", columnList = "total_score DESC"),
        @Index(name = "idx_user_tier", columnList = "tier"),
        @Index(name = "idx_user_node_id", columnList = "node_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private static final Duration FULL_SCAN_COOLDOWN = Duration.ofMinutes(5);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long githubId;

    @Column(unique = true, nullable = false)
    private String nodeId;

    @Column(unique = true, nullable = false)
    private String username;

    private String email;

    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "github_created_at")
    private LocalDateTime githubCreatedAt;

    @Embedded
    private Score score;

    @Embedded
    private RankInfo rankInfo;

    @Column(nullable = false)
    private LocalDateTime lastFullScanAt;

    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public User(Long githubId, String nodeId, String username, String email, String profileImage, LocalDateTime githubCreatedAt, Role role) {
        this.githubId = githubId;
        this.nodeId = nodeId;
        this.username = username;
        this.email = email;
        this.profileImage = profileImage;
        this.githubCreatedAt = githubCreatedAt;
        this.role = role != null ? role : Role.USER;
        this.score = Score.zero();
        this.rankInfo = RankInfo.initial();
        this.lastFullScanAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateActivityStatistics(ActivityStatistics statistics,
                                         long higherScoreCount,
                                         long totalUserCount) {
        this.score = statistics.calculateScore();
        this.rankInfo = RankInfo.calculate(higherScoreCount, totalUserCount, this.score.getValue());
        this.updatedAt = LocalDateTime.now();
    }

    public void updateScore(Score newScore) {
        this.score = newScore;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateRankInfo(RankInfo newRankInfo) {
        this.rankInfo = newRankInfo;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean updateProfile(String newUsername, String newProfileImage, String newEmail) {
        boolean changed = false;

        if (newUsername != null && !newUsername.equals(this.username)) {
            this.username = newUsername;
            changed = true;
        }

        if (newProfileImage != null && !newProfileImage.equals(this.profileImage)) {
            this.profileImage = newProfileImage;
            changed = true;
        }

        if (newEmail != null && !newEmail.equals(this.email)) {
            this.email = newEmail;
            changed = true;
        }

        if (changed) {
            this.updatedAt = LocalDateTime.now();
        }

        return changed;
    }

    public boolean canTriggerFullScan() {
        if (this.lastFullScanAt == null) {
            return true;
        }

        return LocalDateTime.now().isAfter(this.lastFullScanAt.plus(FULL_SCAN_COOLDOWN));
    }

    public LocalDateTime getNextFullScanAvailableAt() {
        if (this.lastFullScanAt == null) {
            return LocalDateTime.now();
        }

        return this.lastFullScanAt.plus(FULL_SCAN_COOLDOWN);
    }

    public void recordFullScan() {
        this.lastFullScanAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isNewUser() {
        return this.score.getValue() == 0 && this.rankInfo.getRanking() == 0;
    }

    public boolean isAtLeast(Tier tier) {
        return this.rankInfo.getTier().ordinal() <= tier.ordinal();
    }

    public int getTotalScore() {
        return score != null ? score.getValue() : 0;
    }

    public int getRanking() {
        return rankInfo != null ? rankInfo.getRanking() : 0;
    }

    public Tier getTier() {
        return rankInfo != null ? rankInfo.getTier() : Tier.IRON;
    }

    public double getPercentile() {
        return rankInfo != null ? rankInfo.getPercentile() : 100.0;
    }
}

