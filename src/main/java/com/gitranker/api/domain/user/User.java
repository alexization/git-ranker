package com.gitranker.api.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nodeId;

    private String username;

    @Column(nullable = false)
    private int totalScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tier tier = Tier.IRON;

    @Column(nullable = false)
    private int ranking = 0;

    @Column(nullable = false)
    private Double percentile = 0.0;

    private String profileImage;

    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public User(String nodeId, String username, String profileImage) {
        this.nodeId = nodeId;
        this.username = username;
        this.profileImage = profileImage;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateUsername(String username) {
        this.username = username;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfileImage(String profileImage) {
        if (profileImage != null && !profileImage.equals(this.profileImage)) {
            this.profileImage = profileImage;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void updateRankInfo(int ranking, Double percentile, Tier tier) {
        this.ranking = ranking;
        this.percentile = percentile;
        this.tier = tier;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateScore(int totalScore) {
        this.totalScore = totalScore;
        this.updatedAt = LocalDateTime.now();
    }
}

