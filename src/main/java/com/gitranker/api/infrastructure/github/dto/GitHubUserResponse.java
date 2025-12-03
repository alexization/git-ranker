package com.gitranker.api.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public record GitHubUserResponse(
        String login,

        @JsonProperty("node_id") String nodeId,

        @JsonProperty("avatar_url") String avatarUrl,

        @JsonProperty("created_at") ZonedDateTime createdAt
) {
}
