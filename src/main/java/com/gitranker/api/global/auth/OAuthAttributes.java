package com.gitranker.api.global.auth;

import com.gitranker.api.domain.user.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record OAuthAttributes(Map<String, Object> attributes,
                              String nameAttributeKey,
                              String username,
                              String nodeId,
                              String profileImage,
                              LocalDateTime githubCreatedAt) {

    public static OAuthAttributes ofGitHub(String userNameAttributeName, Map<String, Object> attributes) {
        String createdAtStr = (String) attributes.get("created_at");
        LocalDateTime githubCreatedAt = LocalDateTime.parse(createdAtStr, DateTimeFormatter.ISO_DATE_TIME);

        return new OAuthAttributes(
                attributes,
                userNameAttributeName,
                (String) attributes.get("login"),
                (String) attributes.get("node_id"),
                (String) attributes.get("avatar_url"),
                githubCreatedAt
        );
    }

    public User toEntity() {
        return User.builder()
                .nodeId(nodeId)
                .username(username)
                .profileImage(profileImage)
                .githubCreatedAt(githubCreatedAt)
                .build();
    }
}
