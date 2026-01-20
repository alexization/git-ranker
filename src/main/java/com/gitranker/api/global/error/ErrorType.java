package com.gitranker.api.global.error;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {
    /* GitHub 관련 에러 */
    GITHUB_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "계정을 찾을 수 없어요. 아이디를 다시 확인해주세요.", LogLevel.INFO),
    GITHUB_COLLECT_ACTIVITY_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "GitHub 활동 데이터를 가져오는데 실패했어요. 잠시 후 다시 시도해주세요.", LogLevel.WARN),
    GITHUB_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "GitHub 서버와 연결할 수 없어요. 잠시 후 다시 시도해주세요.", LogLevel.WARN),
    GITHUB_PARTIAL_ERROR(HttpStatus.MULTI_STATUS, "일부 데이터를 불러오지 못했어요. 랭킹이 정확하지 않을 수 있어요.", LogLevel.WARN),
    GITHUB_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "지금은 요청이 너무 많아요.", LogLevel.WARN),

    /* Batch/API 안정성 관련 에러 */
    GITHUB_API_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "요청 시간이 너무 오래 걸려요. 잠시 후 다시 시도해주세요.", LogLevel.WARN),
    GITHUB_API_CLIENT_ERROR(HttpStatus.BAD_REQUEST, "GitHub 연결 중 문제가 발생했어요.", LogLevel.WARN),
    GITHUB_API_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "GitHub 서버에 문제가 생긴 것 같아요.", LogLevel.ERROR),

    /* Batch 관련 에러 */
    BATCH_JOB_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "작업 처리에 실패했어요. 관리자에게 문의해주세요.", LogLevel.ERROR),
    BATCH_STEP_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 처리 중 문제가 발생했어요.", LogLevel.ERROR),

    /* 공통 에러 */
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했어요. 잠시 후 다시 시도해주세요.", LogLevel.ERROR),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청이에요. 입력 값을 확인해주세요.", LogLevel.INFO),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "접근 권한이 없어요.", LogLevel.INFO),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "등록되지 않은 사용자에요. 먼저 등록해주세요.", LogLevel.INFO),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청하신 정보를 찾을 수 없어요.", LogLevel.INFO),

    /* 인증 관련 에러 */
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "다시 로그인해주세요.", LogLevel.INFO),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "로그인이 만료되었어요. 다시 로그인해주세요.", LogLevel.INFO),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요해요. 로그인해주세요.", LogLevel.INFO),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없어요. 본인의 정보만 수정할 수 있어요.", LogLevel.INFO),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다. 다시 로그인해 주세요.", LogLevel.INFO),

    /* 사용자 관련 에러 */
    REFRESH_COOL_DOWN_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "데이터 갱신은 5분에 한 번만 가능해요.", LogLevel.INFO),
    ACTIVITY_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자의 활동 로그를 찾을 수 없습니다.", LogLevel.INFO),
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
