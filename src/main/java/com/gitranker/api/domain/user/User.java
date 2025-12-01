package com.gitranker.api.domain.user;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
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
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt  = LocalDateTime.now();
}
