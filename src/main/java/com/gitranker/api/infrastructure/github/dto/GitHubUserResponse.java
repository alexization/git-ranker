package com.gitranker.api.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubUserResponse(
        String login,

        @JsonProperty("node_id") String nodeId,

        @JsonProperty("avatar_url") String avatarUrl
) {
}
