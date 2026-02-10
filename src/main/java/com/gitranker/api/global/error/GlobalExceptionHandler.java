package com.gitranker.api.global.error;

import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.exception.GitHubRateLimitException;
import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import com.gitranker.api.global.response.ApiResponse;
import com.gitranker.api.global.util.TimeUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final TimeUtils timeUtils;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        ErrorType errorType = e.getErrorType();

        LogContext ctx = LogContext.event(Event.ERROR_HANDLED)
                .with("error_code", errorType.name())
                .with("error_status", errorType.getStatus().value())
                .with("error_type", e.getClass().getSimpleName())
                .with("error_message", e.getMessage());

        switch (errorType.getLogLevel()) {
            case ERROR -> ctx.error();
            case WARN -> ctx.warn();
            default -> ctx.info();
        }

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType, e.getData()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException e) {
        ErrorType errorType = ErrorType.INVALID_REQUEST;

        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("잘못된 요청입니다");

        LogContext.event(Event.ERROR_HANDLED)
                .with("error_code", errorType.name())
                .with("error_status", errorType.getStatus().value())
                .with("error_type", e.getClass().getSimpleName())
                .with("error_message", message)
                .warn();

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType, message));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        ErrorType errorType = ErrorType.RESOURCE_NOT_FOUND;

        LogContext.event(Event.ERROR_HANDLED)
                .with("error_code", errorType.name())
                .with("error_status", errorType.getStatus().value())
                .with("error_type", e.getClass().getSimpleName())
                .with("error_message", e.getResourcePath())
                .debug();

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType));
    }

    @ExceptionHandler(GitHubRateLimitException.class)
    public ResponseEntity<ApiResponse<Object>> handleGitHubRateLimitException(GitHubRateLimitException e) {
        ErrorType errorType = e.getErrorType();

        String resetTimeStr = timeUtils.formatForDisplay(e.getResetAt().plusMinutes(1));
        String message = String.format("%s 이후에 다시 시도해주세요.", resetTimeStr);

        LogContext.event(Event.ERROR_HANDLED)
                .with("error_code", errorType.name())
                .with("error_status", errorType.getStatus().value())
                .with("error_type", e.getClass().getSimpleName())
                .with("error_message", "ResetAt: " + resetTimeStr)
                .warn();

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        ErrorType errorType = ErrorType.DEFAULT_ERROR;

        LogContext.event(Event.ERROR_HANDLED)
                .with("error_code", errorType.name())
                .with("error_status", errorType.getStatus().value())
                .with("error_type", e.getClass().getSimpleName())
                .with("error_message", e.getMessage())
                .error(e);

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType));
    }
}
