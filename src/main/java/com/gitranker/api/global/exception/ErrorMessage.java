package com.gitranker.api.global.exception;

public class ErrorMessage {
    private final String code;
    private final String message;
    private final Object data;

    private ErrorMessage(String code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public ErrorMessage(ErrorType errorType) {
        this(errorType, null);
    }

    public ErrorMessage(ErrorType errorType, Object data) {
        this(errorType.getCode().name(), errorType.getMessage(), data);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}
