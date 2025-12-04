package com.gitranker.api.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public record GitHubIssueSearchItem(
        Long id,
        String title,
        String state,

        @JsonProperty("created_at")
        ZonedDateTime createdAt,

        @JsonProperty("closed_at")
        ZonedDateTime closedAt,

        @JsonProperty("pull_request")
        PullRequestInfo pullRequest
) {
    public boolean isPullRequest() {
        return pullRequest != null;
    }

    public boolean isMerged() {
        return isPullRequest() && pullRequest.mergedAt != null;
    }

    public record PullRequestInfo(
            @JsonProperty("merged_at")
            ZonedDateTime mergedAt
    ) {

    }
}
