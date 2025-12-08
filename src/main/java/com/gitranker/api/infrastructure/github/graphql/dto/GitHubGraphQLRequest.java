package com.gitranker.api.infrastructure.github.graphql.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubGraphQLRequest(
        @JsonProperty("query")
        String query
) {
    public static GitHubGraphQLRequest of(String query) {
        return new GitHubGraphQLRequest(query);
    }
}
