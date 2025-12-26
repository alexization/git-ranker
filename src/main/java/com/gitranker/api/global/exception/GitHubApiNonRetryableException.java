package com.gitranker.api.global.exception;

import lombok.Getter;

@Getter
public class GitHubApiNonRetryableException extends RuntimeException {
    private final ErrorType errorType;

    public GitHubApiNonRetryableException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }

    public GitHubApiNonRetryableException(ErrorType errorType, String message) {
        super(errorType.getMessage() + ": " + message);
        this.errorType = errorType;
    }
}
