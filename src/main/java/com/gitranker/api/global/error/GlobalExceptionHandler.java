package com.gitranker.api.global.error;

import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.exception.GitHubRateLimitException;
import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import com.gitranker.api.global.metrics.BusinessMetrics;
import com.gitranker.api.global.response.ApiResponse;
import com.gitranker.api.global.util.TimeUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final TimeUtils timeUtils;
    private final BusinessMetrics businessMetrics;
    private final MessageSource messageSource;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        ErrorType errorType = e.getErrorType();
        String localizedMessage = resolveMessage(errorType);

        LogContext ctx = LogContext.event(Event.ERROR_HANDLED)
                .with("error_code", errorType.name())
                .with("error_status", errorType.getStatus().value())
                .with("error_type", e.getClass().getSimpleName())
                .with("error_message", localizedMessage);

        switch (errorType.getLogLevel()) {
            case ERROR -> ctx.error();
            case WARN -> ctx.warn();
            default -> ctx.info();
        }

        businessMetrics.recordError(errorType);

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType, localizedMessage, e.getData()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException e) {
        ErrorType errorType = ErrorType.INVALID_REQUEST;

        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(resolveMessage("error.common.bad-request"));

        LogContext.event(Event.ERROR_HANDLED)
                .with("error_code", errorType.name())
                .with("error_status", errorType.getStatus().value())
                .with("error_type", e.getClass().getSimpleName())
                .with("error_message", message)
                .warn();

        businessMetrics.recordError(errorType);

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType, message, null));
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

        // 404는 크롤러/봇에 의한 노이즈가 대부분이므로 에러 메트릭에서 제외
        
        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType));
    }

    @ExceptionHandler(GitHubRateLimitException.class)
    public ResponseEntity<ApiResponse<Object>> handleGitHubRateLimitException(GitHubRateLimitException e) {
        ErrorType errorType = e.getErrorType();

        String resetTimeStr = timeUtils.formatForDisplay(e.getResetAt().plusMinutes(1));
        String message = resolveMessage("error.github.rate-limit-retry-after", resetTimeStr);

        LogContext.event(Event.ERROR_HANDLED)
                .with("error_code", errorType.name())
                .with("error_status", errorType.getStatus().value())
                .with("error_type", e.getClass().getSimpleName())
                .with("error_message", "ResetAt: " + resetTimeStr)
                .warn();

        businessMetrics.recordError(errorType);

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType, message, null));
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

        businessMetrics.recordError(errorType);

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType));
    }

    private String resolveMessage(ErrorType errorType) {
        return resolveMessage(errorType.getMessageKey());
    }

    private String resolveMessage(String messageKey, Object... args) {
        return messageSource.getMessage(messageKey, args, messageKey, LocaleContextHolder.getLocale());
    }
}
