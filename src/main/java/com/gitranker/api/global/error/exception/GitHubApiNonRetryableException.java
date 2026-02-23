package com.gitranker.api.global.error.exception;

import com.gitranker.api.global.error.ErrorType;
import lombok.Getter;

@Getter
public class GitHubApiNonRetryableException extends RuntimeException {
    private final ErrorType errorType;

    public GitHubApiNonRetryableException(ErrorType errorType) {
        super(errorType.getMessageKey());
        this.errorType = errorType;
    }

    public GitHubApiNonRetryableException(ErrorType errorType, String message) {
        super(errorType.getMessageKey() + ": " + message);
        this.errorType = errorType;
    }
}
