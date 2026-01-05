package com.gitranker.api.global.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class GitHubRateLimitException extends GitHubApiRetryableException {

    private final LocalDateTime resetAt;

    public GitHubRateLimitException(LocalDateTime resetAt) {
        super(ErrorType.GITHUB_API_TIMEOUT, "Rate Limit Exceeded. Reset at: " + resetAt);
        this.resetAt = resetAt;
    }
}
