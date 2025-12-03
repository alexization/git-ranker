package com.gitranker.api.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * GitHub Events API
 * API 문서 : https://docs.github.com/en/rest/activity/events
 */
public record GitHubEventResponse(
        String id,
        String type,

        @JsonProperty("created_at")
        ZonedDateTime createdAt,

        Map<String, Object> payload
) {
    public int getCommitCount() {
        if (!"PushEvent".equals(type)) {
            return 0;
        }

        Object commits = payload.get("commits");
        if (commits instanceof java.util.List) {
            return ((java.util.List<?>) commits).size();
        }
        return 0;
    }

    public boolean isIssueOpened() {
        if (!"IssuesEvent".equals(type)) {
            return false;
        }

        Object action = payload.get("action");
        return "opened".equals(action);
    }

    public boolean isCodeReview() {
        return "PullRequestReviewEvent".equals(type);
    }

    public boolean isPullRequestOpened() {
        if (!"PullRequestEvent".equals(type)) {
            return false;
        }

        Object action = payload.get("action");
        return "opened".equals(action);
    }

    public boolean isPullRequestMerged() {
        if (!"PullRequestEvent".equals(type)) {
            return false;
        }

        Object action = payload.get("action");
        return "merged".equals(action);
    }
}
