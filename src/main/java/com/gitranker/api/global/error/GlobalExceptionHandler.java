package com.gitranker.api.global.error;

import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.exception.GitHubRateLimitException;
import com.gitranker.api.global.response.ApiResponse;
import com.gitranker.api.global.util.TimeUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final TimeUtils timeUtils;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        ErrorType errorType = e.getErrorType();

        switch (errorType.getLogLevel()) {
            case ERROR -> log.error("비즈니스 예외 발생 - Code:{}, Message: {}", errorType.name(), e.getMessage(), e);
            case WARN -> log.warn("비즈니스 경고 발생 - Code: {}, Message: {}", errorType.name(), e.getMessage());
            default -> log.info("비즈니스 이벤트 - Code: {}, Message: {}", errorType.name(), e.getMessage());
        }

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType, e.getData()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .findFirst()
                .orElse("잘못된 요청입니다");

        log.warn("입력값 검증 실패 - Message: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorType.INVALID_REQUEST, message));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        log.debug("리소스 없음 - Path: {}", e.getResourcePath());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorType.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(GitHubRateLimitException.class)
    public ResponseEntity<ApiResponse<Object>> handleGitHubRateLimitException(GitHubRateLimitException e) {
        ErrorType errorType = e.getErrorType();

        String resetTimeStr = timeUtils.formatForDisplay(e.getResetAt().plusMinutes(1));
        String message = String.format("%s 이후에 다시 시도해주세요.", resetTimeStr);

        log.warn("GitHub Rate Limit 도달 - ResetAt: {}", resetTimeStr);

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("시스템 예외 발생 - Type: {}, Message: {}", e.getClass().getSimpleName(), e.getMessage(), e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorType.DEFAULT_ERROR));
    }
}
