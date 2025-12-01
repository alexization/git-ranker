package com.gitranker.api.infrastructure.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubUserResponse(@JsonProperty("node_id") String nodeId, String login,
                                 @JsonProperty("avatar_url") String avatarUrl) {
}
