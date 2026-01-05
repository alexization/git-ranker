package com.gitranker.api.global.error;

import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.exception.GitHubRateLimitException;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.format.DateTimeFormatter;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        ErrorType errorType = e.getErrorType();
        MdcUtils.setError(errorType.name(), e.getMessage());

        switch (errorType.getLogLevel()) {
            case ERROR -> log.error("[Business Error] {} - Code: {}", e.getMessage(), errorType.name(), e);
            case WARN -> log.error("[Business Warning] {} - Code: {}", e.getMessage(), errorType.name());
            default -> log.info("[Business Info]: {} - Code: {}", e.getMessage(), errorType.name());
        }

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType, e.getData()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        MdcUtils.setError("RESOURCE_NOT_FOUND", e.getMessage());
        log.info("[Client Error] Resource Not Found - Path: {}", e.getResourcePath());

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
        MdcUtils.setError(errorType.name(), e.getMessage());

        String resetTimeStr = e.getResetAt()
                .plusMinutes(1)
                .format(DateTimeFormatter.ofPattern("HH:mm"));
        String message = String.format("%s 이후에 다시 시도해주세요.", resetTimeStr);

        log.warn("[GitHub API] Rate Limit Hit - Reset: {}", resetTimeStr);

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        MdcUtils.setError("INTERNAL_SERVER_ERROR", e.getMessage());
        log.error("[Unexpected Error] Internal Server Error - Message: {}", e.getMessage(), e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorType.DEFAULT_ERROR));
    }
}
