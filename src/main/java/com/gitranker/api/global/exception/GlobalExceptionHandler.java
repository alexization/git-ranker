package com.gitranker.api.global.exception;

import com.gitranker.api.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        ErrorType errorType = e.getErrorType();

        if (e.getErrorType().getLogLevel() == LogLevel.ERROR) {
            log.error("Business Exception: {}", e.getMessage(), e);
        } else {
            log.warn("Business Exception: {}", e.getMessage());
        }

        return ResponseEntity
                .status(errorType.getStatus())
                .body(ApiResponse.error(errorType, e.getData()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("Resource Not Found: {}", e.getResourcePath());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorType.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("Unexpected Error: {}", e.getMessage(), e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorType.DEFAULT_ERROR));
    }
}
