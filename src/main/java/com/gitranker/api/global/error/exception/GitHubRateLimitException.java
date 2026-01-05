package com.gitranker.api.global.error.exception;

import com.gitranker.api.global.error.ErrorType;
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
