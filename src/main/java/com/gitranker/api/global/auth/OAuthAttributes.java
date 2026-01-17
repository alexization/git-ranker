package com.gitranker.api.global.auth;

import com.gitranker.api.domain.user.Role;
import com.gitranker.api.domain.user.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record OAuthAttributes(
        Map<String, Object> attributes,
        String nameAttributeKey,
        Long githubId,
        String nodeId,
        String username,
        String email,
        String profileImage,
        LocalDateTime githubCreatedAt
) {
    public static OAuthAttributes of(String userNameAttributeName, Map<String, Object> attributes) {
        String createdAtStr = (String) attributes.get("created_at");
        LocalDateTime createdAt = LocalDateTime.parse(createdAtStr, DateTimeFormatter.ISO_DATE_TIME);

        return new OAuthAttributes(
                attributes,
                userNameAttributeName,
                ((Integer) attributes.get("id")).longValue(),
                (String) attributes.get("node_id"),
                (String) attributes.get("login"),
                (String) attributes.get("email"),
                (String) attributes.get("avatar_url"),
                createdAt
        );
    }

    public User toEntity() {
        return User.builder()
                .githubId(githubId)
                .nodeId(nodeId)
                .username(username)
                .email(email)
                .profileImage(profileImage)
                .githubCreatedAt(githubCreatedAt)
                .role(Role.USER)
                .build();
    }
}