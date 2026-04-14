package com.gitranker.api.support;

import com.gitranker.api.domain.auth.RefreshToken;
import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.user.Role;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.auth.OAuthAttributes;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static User user() {
        return user("testuser");
    }

    public static User user(String username) {
        return User.builder()
                .githubId(1L)
                .nodeId("node-" + username)
                .username(username)
                .email(username + "@example.com")
                .profileImage("https://images.example.com/" + username + ".png")
                .githubCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .role(Role.USER)
                .build();
    }

    public static User savedUser(long id, String username) {
        User user = user(username);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static ActivityStatistics stats(int commits, int issues, int openedPrs, int mergedPrs, int reviews) {
        return ActivityStatistics.of(commits, issues, openedPrs, mergedPrs, reviews);
    }

    public static ActivityLog emptyActivityLog(User user) {
        return ActivityLog.empty(user, LocalDate.of(2025, 1, 1));
    }

    public static ActivityLog activityLog(User user, ActivityStatistics totals, ActivityStatistics diff) {
        return ActivityLog.of(user, totals, diff, LocalDate.of(2025, 1, 1));
    }

    public static RefreshToken refreshToken(User user, String token, LocalDateTime expiresAt) {
        return RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(expiresAt)
                .build();
    }

    public static OAuthAttributes oauthAttributes(String username, String email, String profileImage) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345);
        attributes.put("node_id", "MDQ6VXNlcjEyMzQ1");
        attributes.put("login", username);
        attributes.put("email", email);
        attributes.put("avatar_url", profileImage);
        attributes.put("created_at", "2020-01-01T00:00:00Z");
        return OAuthAttributes.of("id", attributes);
    }
}
