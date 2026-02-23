package com.gitranker.api.global.error;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {
    /* GitHub 관련 에러 */
    GITHUB_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.github.user-not-found", LogLevel.INFO),
    GITHUB_COLLECT_ACTIVITY_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "error.github.collect-activity-failed", LogLevel.WARN),
    GITHUB_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "error.github.api-error", LogLevel.WARN),
    GITHUB_PARTIAL_ERROR(HttpStatus.MULTI_STATUS, "error.github.partial-error", LogLevel.WARN),
    GITHUB_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "error.github.rate-limit-exceeded", LogLevel.WARN),
    GITHUB_RATE_LIMIT_EXHAUSTED(HttpStatus.SERVICE_UNAVAILABLE, "error.github.rate-limit-exhausted", LogLevel.WARN),

    /* Batch/API 안정성 관련 에러 */
    GITHUB_API_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "error.github.api-timeout", LogLevel.WARN),
    GITHUB_API_CLIENT_ERROR(HttpStatus.BAD_REQUEST, "error.github.api-client-error", LogLevel.WARN),
    GITHUB_API_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "error.github.api-server-error", LogLevel.ERROR),

    /* Batch 관련 에러 */
    BATCH_JOB_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "error.batch.job-failed", LogLevel.ERROR),
    BATCH_STEP_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "error.batch.step-failed", LogLevel.ERROR),

    /* 공통 에러 */
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error.common.default", LogLevel.ERROR),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "error.common.invalid-request", LogLevel.INFO),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "error.common.unauthorized-access", LogLevel.INFO),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.user.not-found", LogLevel.INFO),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "error.common.resource-not-found", LogLevel.INFO),

    /* 인증 관련 에러 */
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "error.auth.invalid-refresh-token", LogLevel.INFO),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "error.auth.expired-refresh-token", LogLevel.INFO),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "error.auth.unauthorized", LogLevel.INFO),
    FORBIDDEN(HttpStatus.FORBIDDEN, "error.auth.forbidden", LogLevel.INFO),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "error.auth.session-expired", LogLevel.INFO),

    /* 사용자 관련 에러 */
    REFRESH_COOL_DOWN_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "error.user.refresh-cooldown-exceeded", LogLevel.INFO),
    ACTIVITY_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "error.user.activity-log-not-found", LogLevel.INFO),
    ;

    private final HttpStatus status;
    private final String messageKey;
    private final LogLevel logLevel;

    ErrorType(HttpStatus status, String messageKey, LogLevel logLevel) {
        this.status = status;
        this.messageKey = messageKey;
        this.logLevel = logLevel;
    }
}
