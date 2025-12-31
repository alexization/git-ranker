package com.gitranker.api.global.exception;

import lombok.Getter;

@Getter
public class GitHubRateLimitException extends GitHubApiRetryableException {

    private final long waitTimeMs;

    public GitHubRateLimitException(long waitTimeMs) {
        super(ErrorType.GITHUB_API_TIMEOUT, "Rate Limit Exceeded. Wait for " + waitTimeMs + "ms");
        this.waitTimeMs = waitTimeMs;
    }
}
