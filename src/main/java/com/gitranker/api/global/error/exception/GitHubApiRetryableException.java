package com.gitranker.api.global.error.exception;

import com.gitranker.api.global.error.ErrorType;
import lombok.Getter;

@Getter
public class GitHubApiRetryableException extends RuntimeException {
    private final ErrorType errorType;

    public GitHubApiRetryableException(ErrorType errorType) {
        super(errorType.getMessageKey());
        this.errorType = errorType;
    }

    public GitHubApiRetryableException(ErrorType errorType, String message) {
        super(errorType.getMessageKey() + ": " + message);
        this.errorType = errorType;
    }

    public GitHubApiRetryableException(ErrorType errorType, Throwable cause) {
        super(errorType.getMessageKey(), cause);
        this.errorType = errorType;
    }
}
