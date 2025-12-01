package com.gitranker.api.global.exception;

public class BusinessException extends RuntimeException {
    private final ErrorType errorType;
    private final Object data;

    public BusinessException(ErrorType errorType) {
        this(errorType, null);
    }

    public BusinessException(ErrorType errorType, Object data) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.data = data;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public Object getData() {
        return data;
    }
}
