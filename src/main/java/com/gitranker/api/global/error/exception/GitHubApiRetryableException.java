package com.gitranker.api.global.error.exception;

import com.gitranker.api.global.error.ErrorType;
import lombok.Getter;

@Getter
public class GitHubApiRetryableException extends RuntimeException {
    private final ErrorType errorType;

    public GitHubApiRetryableException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }

    public GitHubApiRetryableException(ErrorType errorType, String message) {
        super(errorType.getMessage() + ": " + message);
        this.errorType = errorType;
    }

    public GitHubApiRetryableException(ErrorType errorType, Throwable cause) {
        super(errorType.getMessage(), cause);
        this.errorType = errorType;
    }
}
