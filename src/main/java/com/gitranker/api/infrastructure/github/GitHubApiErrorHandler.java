package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import com.gitranker.api.global.error.exception.GitHubRateLimitException;
import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.RequiredArgsConstructor;
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
            LogContext.event(Event.GITHUB_API_CALLED)
                    .with("target", "github_api")
                    .with("outcome", "failure")
                    .with("error_type", "USER_NOT_FOUND")
                    .warn();

            throw new GitHubApiNonRetryableException(ErrorType.GITHUB_USER_NOT_FOUND);
        }

        LogContext.event(Event.GITHUB_API_CALLED)
                .with("target", "github_api")
                .with("outcome", "failure")
                .with("error_type", "GRAPHQL_PARTIAL_ERROR")
                .warn();

        throw new GitHubApiRetryableException(ErrorType.GITHUB_PARTIAL_ERROR);
    }

    public GitHubApiRetryableException handleTimeout(TimeoutException e, Duration timeout) {
        LogContext.event(Event.GITHUB_API_CALLED)
                .with("target", "github_api")
                .with("outcome", "failure")
                .with("error_type", "TIMEOUT")
                .with("timeout_seconds", timeout.getSeconds())
                .warn();

        return new GitHubApiRetryableException(ErrorType.GITHUB_API_TIMEOUT, e);
    }

    public GitHubApiRetryableException handleReadTimeout(ReadTimeoutException e) {
        LogContext.event(Event.GITHUB_API_CALLED)
                .with("target", "github_api")
                .with("outcome", "failure")
                .with("error_type", "READ_TIMEOUT")
                .warn();

        return new GitHubApiRetryableException(ErrorType.GITHUB_API_TIMEOUT, e);
    }

    public GitHubApiRetryableException handleIOException(IOException e) {
        LogContext.event(Event.GITHUB_API_CALLED)
                .with("target", "github_api")
                .with("outcome", "failure")
                .with("error_type", "IO_ERROR")
                .with("error_message", e.getMessage())
                .warn();

        return new GitHubApiRetryableException(ErrorType.GITHUB_API_ERROR, e);
    }

    public GitHubApiRetryableException handleNetworkError(WebClientRequestException e) {
        LogContext.event(Event.GITHUB_API_CALLED)
                .with("target", "github_api")
                .with("outcome", "failure")
                .with("error_type", "NETWORK_ERROR")
                .with("error_message", e.getMessage())
                .warn();

        return new GitHubApiRetryableException(ErrorType.GITHUB_API_ERROR, e);
    }

    private GitHubRateLimitException handleRateLimitExceeded(ClientResponse response, int statusValue) {
        LocalDateTime resetAt = parseResetTime(response.headers());

        LogContext.event(Event.GITHUB_API_CALLED)
                .with("target", "github_api")
                .with("outcome", "failure")
                .with("error_type", "RATE_LIMIT_EXCEEDED")
                .with("status", statusValue)
                .with("reset_at", resetAt.toString())
                .warn();

        apiMetrics.recordRateLimitExceeded();

        return new GitHubRateLimitException(resetAt);
    }

    private GitHubApiRetryableException handleClientError(int statusValue) {
        LogContext.event(Event.GITHUB_API_CALLED)
                .with("target", "github_api")
                .with("outcome", "failure")
                .with("error_type", "CLIENT_ERROR")
                .with("status", statusValue)
                .error();

        apiMetrics.recordFailure();

        return new GitHubApiRetryableException(ErrorType.GITHUB_API_CLIENT_ERROR, "Status: " + statusValue);
    }

    private GitHubApiRetryableException handleServerError(int statusValue) {
        LogContext.event(Event.GITHUB_API_CALLED)
                .with("target", "github_api")
                .with("outcome", "failure")
                .with("error_type", "SERVER_ERROR")
                .with("status", statusValue)
                .error();

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
