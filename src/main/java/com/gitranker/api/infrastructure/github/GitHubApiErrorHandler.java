package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import com.gitranker.api.global.error.exception.GitHubRateLimitException;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ClientResponse.Headers;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubApiErrorHandler {

    private final ZoneId appZoneId;
    private final GitHubApiMetrics apiMetrics;

    public RuntimeException handleHttpStatus(ClientResponse response) {
        HttpStatusCode statusCode = response.statusCode();
        int statusValue = statusCode.value();

        if (statusValue == 403 || statusValue == 429) {
            return handleRateLimitExceeded(response, statusValue);
        }
        if (statusCode.is4xxClientError()) {
            return handleClientError(statusValue);
        }
        if (statusCode.is5xxServerError()) {
            return handleServerError(statusValue);
        }

        return null;
    }

    public void handleGraphQLErrors(List<Object> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }

        String errorString = errors.toString();

        if (errorString.contains("Could not resolve to a User")) {
            MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
            MdcUtils.setError(ErrorType.GITHUB_USER_NOT_FOUND.name(), "사용자를 찾을 수 없음");

            log.warn("GitHub 사용자 조회 실패 - 사용자를 찾을 수 없습니다.");

            throw new GitHubApiNonRetryableException(ErrorType.GITHUB_USER_NOT_FOUND);
        }

        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
        MdcUtils.setError("GraphQLError", errorString);

        log.warn("GraphQL 부분 에러 발생 - Errors: {}", errors);

        throw new GitHubApiRetryableException(ErrorType.GITHUB_PARTIAL_ERROR);
    }

    public GitHubApiRetryableException handleTimeout(TimeoutException e, Duration timeout) {
        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
        MdcUtils.setError("TimeoutException", "API 호출 타임아웃: " + timeout.getSeconds());

        log.warn("API 타임아웃 발생 - Timeout: {}초", timeout.getSeconds());

        return new GitHubApiRetryableException(ErrorType.GITHUB_API_TIMEOUT, e);
    }

    public GitHubApiRetryableException handleReadTimeout(ReadTimeoutException e) {
        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
        MdcUtils.setError("ReadTimeoutException", "읽기 타임아웃");

        log.warn("읽기 타임아웃 발생");

        return new GitHubApiRetryableException(ErrorType.GITHUB_API_TIMEOUT, e);
    }

    public GitHubApiRetryableException handleIOException(IOException e) {
        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
        MdcUtils.setError("IOException", e.getMessage());

        log.warn("IO 에러 발생 - {}", e.getMessage());

        return new GitHubApiRetryableException(ErrorType.GITHUB_API_ERROR, e);
    }

    public GitHubApiRetryableException handleNetworkError(WebClientRequestException e) {
        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
        MdcUtils.setError("NetworkError", e.getMessage());

        log.warn("네트워크 에러 발생 - {}", e.getMessage());

        throw new GitHubApiRetryableException(ErrorType.GITHUB_API_ERROR, e);
    }

    private GitHubRateLimitException handleRateLimitExceeded(ClientResponse response, int statusValue) {
        LocalDateTime resetAt = parseResetTime(response.headers());

        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.API_RATE_THRESHOLD);
        log.warn("Rate Limit 초과 - Status: {}, ResetAt: {}", statusValue, resetAt);

        apiMetrics.recordRateLimitExceeded();

        return new GitHubRateLimitException(resetAt);
    }

    private GitHubApiRetryableException handleClientError(int statusValue) {
        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.API_FAILED);
        log.error("API 클라이언트 에러 - Status: {}", statusValue);

        apiMetrics.recordFailure();

        return new GitHubApiRetryableException(ErrorType.GITHUB_API_CLIENT_ERROR, "Status: " + statusValue);
    }

    private GitHubApiRetryableException handleServerError(int statusValue) {
        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.API_FAILED);
        log.error("API 서버 에러 - Status: {}", statusValue);

        apiMetrics.recordFailure();

        return new GitHubApiRetryableException(ErrorType.GITHUB_API_SERVER_ERROR, "Status: " + statusValue);
    }

    private LocalDateTime parseResetTime(Headers headers) {
        return headers.header("x-ratelimit-reset").stream()
                .findFirst()
                .map(Long::parseLong)
                .map(epoch -> LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), appZoneId))
                .orElseGet(() -> LocalDateTime.now(appZoneId).plusMinutes(60));
    }
}
