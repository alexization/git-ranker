package com.gitranker.api.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record GitHubNodeUserResponse(
        @JsonProperty("data")
        Data data
) {
    public String getLogin() {
        return data.node().login();
    }

    public String getAvatarUrl() {
        return data.node().avatarUrl();
    }

    public boolean hasUser() {
        return data != null && data.node() != null && data.node().login() != null;
    }

    public record Data(
            @JsonProperty("node") Node node,
            @JsonProperty("rateLimit") RateLimit rateLimit
    ) {
    }

    public record Node(
            @JsonProperty("id")
            String id,

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
    ) implements GitHubRateLimitInfo {
    }
}
