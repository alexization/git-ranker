package com.gitranker.api.global.error;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {
    /* GitHub 관련 에러 */
    GITHUB_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 GitHub 계정입니다.", LogLevel.INFO),
    GITHUB_COLLECT_ACTIVITY_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "사용자의 GitHub 활동 조회를 실패했습니다.", LogLevel.WARN),
    GITHUB_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "GitHub API 호출에 실패했습니다.", LogLevel.WARN),
    GITHUB_PARTIAL_ERROR(HttpStatus.MULTI_STATUS, "GitHub 데이터 중 일부를 불러오지 못했습니다.", LogLevel.WARN),
    GITHUB_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "GitHub API 요청 한도가 초과되었습니다.", LogLevel.WARN),

    /* Batch/API 안정성 관련 에러 */
    GITHUB_API_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "GitHub API 요청 시간이 초과되었습니다.", LogLevel.WARN),
    GITHUB_API_CLIENT_ERROR(HttpStatus.BAD_REQUEST, "GitHub API 클라이언트 오류가 발생했습니다.", LogLevel.WARN),
    GITHUB_API_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "GitHub API 서버 오류가 발생했습니다.", LogLevel.ERROR),

    /* Batch 관련 에러 */
    BATCH_JOB_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배치 작업 실행 중 오류가 발생했습니다.", LogLevel.ERROR),
    BATCH_STEP_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배치 상세 단계 처리 중 오류가 발생했습니다.", LogLevel.ERROR),

    /* 공통 에러 */
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 발생했습니다.", LogLevel.ERROR),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청이 올바르지 않습니다.", LogLevel.INFO),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "인증되지 않은 접근입니다.", LogLevel.INFO),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자 입니다.", LogLevel.INFO),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.", LogLevel.INFO),

    /* 사용자 관련 */
    REFRESH_COOL_DOWN_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "전체 갱신은 7일에 1회만 가능합니다.", LogLevel.INFO),
    ;

    private final HttpStatus status;
    private final String message;
    private final LogLevel logLevel;

    ErrorType(HttpStatus status, String message, LogLevel logLevel) {
        this.status = status;
        this.message = message;
        this.logLevel = logLevel;
    }
}
