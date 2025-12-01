package com.gitranker.api.global.exception;

import com.gitranker.api.global.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        String logLevelName = e.getErrorType().getLogLevel().name();

        switch (logLevelName) {
            case "ERROR":
                logger.error("Business Exception: {} - {}", e.getErrorType().getCode(), e.getMessage(), e);
                break;
            case "WARN":
                logger.warn("Business Exception: {} - {}", e.getErrorType().getCode(), e.getMessage());
                break;
            default:
                logger.info("Business Exception: {} - {}", e.getErrorType().getCode(), e.getMessage());
                break;
        }

        ApiResponse<Object> response = ApiResponse.error(e.getErrorType(), e.getData());
        return ResponseEntity.status(e.getErrorType().getStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        logger.error("Unexpected Exception: {}", e.getMessage(), e);

        ApiResponse<Object> response = ApiResponse.error(ErrorType.DEFAULT_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
