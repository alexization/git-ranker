package com.gitranker.api.global.exception;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {
    /* 1xxx GitHub 관련 커스텀 에러 */
    GITHUB_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "E1001", "존재하지 않는 GitHub 계정입니다.", LogLevel.INFO),
    GITHUB_COLLECT_ACTIVITY_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "E1002", "사용자의 GitHub 활동 조회를 실패했습니다.", LogLevel.WARN),
    GITHUB_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "E1003", "GitHub API 호출에 실패했습니다.", LogLevel.WARN),

    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500", "알 수 없는 오류가 발생했습니다.", LogLevel.ERROR),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "E400", "요청이 올바르지 않습니다.", LogLevel.INFO),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "E401", "인증되지 않은 접근입니다.", LogLevel.WARN),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "존재하지 않는 사용자 입니다.", LogLevel.INFO),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "요청한 리소스를 찾을 수 없습니다.", LogLevel.WARN);

    private final HttpStatus status;
    private final String code;
    private final String message;
    private final LogLevel logLevel;

    ErrorType(HttpStatus status, String code, String message, LogLevel logLevel) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.logLevel = logLevel;
    }
}
