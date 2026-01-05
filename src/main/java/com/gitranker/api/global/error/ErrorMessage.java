package com.gitranker.api.global.error;

import lombok.Getter;

@Getter
public class ErrorMessage {
    private final String type;
    private final String message;
    private final Object data;

    private ErrorMessage(String type, String message, Object data) {
        this.type = type;
        this.message = message;
        this.data = data;
    }

    public ErrorMessage(ErrorType errorType) {
        this(errorType, null);
    }

    public ErrorMessage(ErrorType errorType, Object data) {
        this(errorType.toString(), errorType.getMessage(), data);
    }
}
