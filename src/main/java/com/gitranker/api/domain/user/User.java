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

    private int totalScore;

    @Enumerated(EnumType.STRING)
    private Tier tier;

    private int ranking;

    private Double percentile;

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
        if (profileImage != null && profileImage.equals(this.profileImage)) {
            this.profileImage = profileImage;
            this.updatedAt = LocalDateTime.now();
        }
    }
}

