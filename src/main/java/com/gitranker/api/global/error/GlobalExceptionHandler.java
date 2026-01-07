package com.gitranker.api.global.error;

import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.exception.GitHubRateLimitException;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.global.response.ApiResponse;
import com.gitranker.api.global.util.TimeUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final TimeUtils timeUtils;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        ErrorType errorType = e.getErrorType();

        MdcUtils.setLogContext(LogCategory.DOMAIN, EventType.FAILURE);
        MdcUtils.setError(errorType.name(), e.getMessage());

        switch (errorType.getLogLevel()) {
            case ERROR -> log.error("비즈니스 예외 발생 - Code:{}, Message: {}", errorType.name(), e.getMessage(), e);
            case WARN -> log.error("비즈니스 경고 발생 - Code: {}, Message: {}", errorType.name(), e.getMessage());
            default -> log.info("비즈니스 이벤트 - Code: {}, Message: {}", errorType.name(), e.getMessage());
        }

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType, e.getData()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        MdcUtils.setLogContext(LogCategory.HTTP, EventType.FAILURE);
        MdcUtils.setError("RESOURCE_NOT_FOUND", e.getMessage());

        log.info("리소스 없음 - Path: {}", e.getResourcePath());

        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ErrorType.RESOURCE_NOT_FOUND));
        }

        return "error/404";
    }

    @ExceptionHandler(GitHubRateLimitException.class)
    public ResponseEntity<ApiResponse<Object>> handleGitHubRateLimitException(GitHubRateLimitException e) {
        ErrorType errorType = e.getErrorType();

        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
        MdcUtils.setError(errorType.name(), e.getMessage());

        String resetTimeStr = timeUtils.formatForDisplay(e.getResetAt().plusMinutes(1));
        String message = String.format("%s 이후에 다시 시도해주세요.", resetTimeStr);

        log.warn("GitHub Rate Limit 도달 - ResetAt: {}", resetTimeStr);

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        MdcUtils.setLogContext(LogCategory.SYSTEM, EventType.FAILURE);
        MdcUtils.setError("INTERNAL_SERVER_ERROR", e.getMessage());

        log.error("시스템 예외 발생 - Type: {}, Message: {}", e.getClass().getSimpleName(), e.getMessage(), e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorType.DEFAULT_ERROR));
    }
}
