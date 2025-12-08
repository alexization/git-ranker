package com.gitranker.api.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public record GitHubUserInfoResponse(
        @JsonProperty("data")
        Data data
) {
    public LocalDateTime getGitHubCreatedAt() {
        String createdAt = data.user().createdAt();

        return ZonedDateTime.parse(createdAt).toLocalDateTime();
    }

    public String getLogin() {
        return data.user().login();
    }

    public String getAvatarUrl() {
        return data.user().avatarUrl();
    }

    public String getNodeId() {
        return data.user().id();
    }

    public record Data(
            @JsonProperty("user")
            User user
    ) {
    }

    public record User(
            @JsonProperty("id")
            String id,

            @JsonProperty("createdAt")
            String createdAt,

            @JsonProperty("login")
            String login,

            @JsonProperty("avatarUrl")
            String avatarUrl
    ) {
    }
}
