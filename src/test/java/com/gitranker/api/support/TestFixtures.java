package com.gitranker.api.support;

import com.gitranker.api.domain.auth.RefreshToken;
import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.user.Role;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.auth.OAuthAttributes;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public final class TestFixtures {

    private static final long DEFAULT_GITHUB_ID = 1L;
    private static final LocalDateTime DEFAULT_GITHUB_CREATED_AT = LocalDateTime.of(2020, 1, 1, 0, 0);
    private static final LocalDate FIXED_ACTIVITY_DATE = LocalDate.of(2025, 1, 1);
    private static final int FIXED_OAUTH_GITHUB_ID = 12345;
    private static final String FIXED_OAUTH_NODE_ID = "MDQ6VXNlcjEyMzQ1";
    private static final String FIXED_OAUTH_CREATED_AT = "2020-01-01T00:00:00Z";

    private TestFixtures() {
    }

    public static User user() {
        return user("testuser");
    }

    public static User user(String username) {
        return new User(
                DEFAULT_GITHUB_ID,
                "node-" + username,
                username,
                username + "@example.com",
                "https://images.example.com/" + username + ".png",
                DEFAULT_GITHUB_CREATED_AT,
                Role.USER
        );
    }

    public static User savedUser(long id, String username) {
        User user = user(username);
        setField(user, "id", id);
        return user;
    }

    public static ActivityStatistics stats(int commits, int issues, int openedPrs, int mergedPrs, int reviews) {
        return ActivityStatistics.of(commits, issues, openedPrs, mergedPrs, reviews);
    }

    public static ActivityLog emptyActivityLog(User user) {
        return ActivityLog.empty(user, FIXED_ACTIVITY_DATE);
    }

    public static ActivityLog activityLog(User user, ActivityStatistics totals, ActivityStatistics diff) {
        return ActivityLog.of(user, totals, diff, FIXED_ACTIVITY_DATE);
    }

    public static RefreshToken refreshToken(User user, String token, LocalDateTime expiresAt) {
        return new RefreshToken(token, user, expiresAt);
    }

    public static OAuthAttributes oauthAttributes(String username, String email, String profileImage) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", FIXED_OAUTH_GITHUB_ID);
        attributes.put("node_id", FIXED_OAUTH_NODE_ID);
        attributes.put("login", username);
        attributes.put("email", email);
        attributes.put("avatar_url", profileImage);
        attributes.put("created_at", FIXED_OAUTH_CREATED_AT);
        return OAuthAttributes.of("id", attributes);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("테스트 fixture field 설정에 실패했습니다: " + fieldName, exception);
        }
    }
}
