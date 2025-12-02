package com.gitranker.api.global.exception;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "알 수 없는 오류가 발생했습니다.", LogLevel.ERROR),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, ErrorCode.E400, "요청이 올바르지 않습니다.", LogLevel.INFO),

    /* F-001 */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.E404, "존재하지 않는 GitHub 계정입니다.", LogLevel.INFO),
    GITHUB_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.E503, "GitHub API 호출에 실패했습니다.", LogLevel.WARN);

    private final HttpStatus status;
    private final ErrorCode code;
    private final String message;
    private final LogLevel logLevel;

    ErrorType(HttpStatus status, ErrorCode code, String message, LogLevel logLevel) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.logLevel = logLevel;
    }

}
