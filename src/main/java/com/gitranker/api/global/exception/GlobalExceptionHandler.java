package com.gitranker.api.global.exception;

import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFoundException(NoResourceFoundException e) {
        MdcUtils.setError("RESOURCE_NOT_FOUND", e.getMessage());
        log.info("[Client Error] Resource Not Found - Path: {}", e.getResourcePath());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorType.RESOURCE_NOT_FOUND));
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
