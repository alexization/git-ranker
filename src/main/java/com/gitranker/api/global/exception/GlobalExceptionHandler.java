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
        MDC.put("error_code", e.getErrorType().getCode());

        if (e.getErrorType().getLogLevel() == LogLevel.ERROR) {
            log.error("Business Exception: {}", e.getMessage(), e);
        } else {
            log.warn("Business Exception: {}", e.getMessage());
        }

        ApiResponse<Object> response = ApiResponse.error(e.getErrorType(), e.getData());
        return ResponseEntity.status(e.getErrorType().getStatus()).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFoundException(NoResourceFoundException e) {
        MDC.put("error_code", "E404");

        String requestedResource = e.getResourcePath();
        log.warn("Resource Not Found: {}", requestedResource);

        ApiResponse<Object> response = ApiResponse.error(ErrorType.RESOURCE_NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        MDC.put("error_code", "E500");
        log.error("Unexpected Error: {}", e.getMessage(), e);

        ApiResponse<Object> response = ApiResponse.error(ErrorType.DEFAULT_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
