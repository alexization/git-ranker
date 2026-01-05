package com.gitranker.api.global.error;

import lombok.Getter;

@Getter
public class ErrorMessage {
    private final String message;
    private final Object data;

    private ErrorMessage(String message, Object data) {
        this.message = message;
        this.data = data;
    }

    public ErrorMessage(ErrorType errorType) {
        this(errorType, null);
    }

    public ErrorMessage(ErrorType errorType, Object data) {
        this(errorType.getMessage(), data);
    }
}
