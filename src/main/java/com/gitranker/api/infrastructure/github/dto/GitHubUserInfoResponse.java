package com.gitranker.api.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gitranker.api.global.exception.BusinessException;
import com.gitranker.api.global.exception.ErrorType;

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
        if (data.user() == null || data.user().id() == null) {
            throw new BusinessException(ErrorType.GITHUB_USER_NOT_FOUND);
        }

        return data.user().id();
    }

    public record Data(
            @JsonProperty("user") User user,
            @JsonProperty("rateLimit") RateLimit rateLimit
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

    public record RateLimit(
            int limit,
            int cost,
            int remaining,
            LocalDateTime resetAt
    ) {
    }
}
