package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import com.gitranker.api.global.error.exception.GitHubRateLimitException;
import io.netty.handler.timeout.ReadTimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GitHubApiErrorHandlerTest {

    @Mock
    private GitHubApiMetrics apiMetrics;

    @Test
    @DisplayName("handleHttpStatus returns rate-limit exception with parsed reset time")
    void handlesRateLimitStatusWithResetHeader() {
        GitHubApiErrorHandler handler = new GitHubApiErrorHandler(ZoneId.of("Asia/Seoul"), apiMetrics);
        long resetEpoch = LocalDateTime.of(2026, 4, 16, 18, 0)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toEpochSecond();
        ClientResponse response = ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS)
                .header("x-ratelimit-reset", String.valueOf(resetEpoch))
                .build();

        RuntimeException actual = handler.handleHttpStatus(response);

        assertThat(actual).isInstanceOf(GitHubRateLimitException.class);
        assertThat(((GitHubRateLimitException) actual).getResetAt())
                .isEqualTo(LocalDateTime.of(2026, 4, 16, 18, 0));
        verify(apiMetrics).recordRateLimitExceeded();
    }

    @Test
    @DisplayName("handleHttpStatus falls back to about one hour when reset header is missing")
    void fallsBackWhenResetHeaderMissing() {
        GitHubApiErrorHandler handler = new GitHubApiErrorHandler(ZoneId.of("Asia/Seoul"), apiMetrics);
        LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        RuntimeException actual = handler.handleHttpStatus(ClientResponse.create(HttpStatus.FORBIDDEN).build());

        LocalDateTime resetAt = ((GitHubRateLimitException) actual).getResetAt();
        assertThat(actual).isInstanceOf(GitHubRateLimitException.class);
        assertThat(resetAt).isAfterOrEqualTo(before.plusMinutes(59));
        assertThat(resetAt).isBeforeOrEqualTo(before.plusMinutes(61));
        verify(apiMetrics).recordRateLimitExceeded();
    }

    @Test
    @DisplayName("handleHttpStatus translates client and server failures to retryable GitHub errors")
    void translatesClientAndServerErrors() {
        GitHubApiErrorHandler handler = new GitHubApiErrorHandler(ZoneId.of("Asia/Seoul"), apiMetrics);

        RuntimeException clientError = handler.handleHttpStatus(ClientResponse.create(HttpStatus.BAD_REQUEST).build());
        RuntimeException serverError = handler.handleHttpStatus(ClientResponse.create(HttpStatus.BAD_GATEWAY).build());

        assertThat(clientError).isInstanceOf(GitHubApiRetryableException.class);
        assertThat(((GitHubApiRetryableException) clientError).getErrorType()).isEqualTo(ErrorType.GITHUB_API_CLIENT_ERROR);
        assertThat(serverError).isInstanceOf(GitHubApiRetryableException.class);
        assertThat(((GitHubApiRetryableException) serverError).getErrorType()).isEqualTo(ErrorType.GITHUB_API_SERVER_ERROR);
        verify(apiMetrics, org.mockito.Mockito.times(2)).recordFailure();
    }

    @Test
    @DisplayName("handleHttpStatus returns null for successful status")
    void returnsNullForSuccessfulStatus() {
        GitHubApiErrorHandler handler = new GitHubApiErrorHandler(ZoneId.of("Asia/Seoul"), apiMetrics);

        assertThat(handler.handleHttpStatus(ClientResponse.create(HttpStatus.OK).build())).isNull();
    }

    @Test
    @DisplayName("handleGraphQLErrors ignores null and empty errors")
    void ignoresEmptyGraphQLErrors() {
        GitHubApiErrorHandler handler = new GitHubApiErrorHandler(ZoneId.of("Asia/Seoul"), apiMetrics);

        assertThatCode(() -> handler.handleGraphQLErrors(null)).doesNotThrowAnyException();
        assertThatCode(() -> handler.handleGraphQLErrors(List.of())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleGraphQLErrors translates unresolved user errors to non-retryable exception")
    void translatesUserNotFoundGraphQLError() {
        GitHubApiErrorHandler handler = new GitHubApiErrorHandler(ZoneId.of("Asia/Seoul"), apiMetrics);

        assertThatThrownBy(() -> handler.handleGraphQLErrors(List.of("Could not resolve to a User with the login")))
                .isInstanceOf(GitHubApiNonRetryableException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.GITHUB_USER_NOT_FOUND);
    }

    @Test
    @DisplayName("handleGraphQLErrors translates other GraphQL errors to retryable exception")
    void translatesPartialGraphQLError() {
        GitHubApiErrorHandler handler = new GitHubApiErrorHandler(ZoneId.of("Asia/Seoul"), apiMetrics);

        assertThatThrownBy(() -> handler.handleGraphQLErrors(List.of("some partial error")))
                .isInstanceOf(GitHubApiRetryableException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.GITHUB_PARTIAL_ERROR);
    }

    @Test
    @DisplayName("timeout-related helpers map to retryable GitHub exceptions with causes")
    void mapsTimeoutAndIoExceptions() {
        GitHubApiErrorHandler handler = new GitHubApiErrorHandler(ZoneId.of("Asia/Seoul"), apiMetrics);
        TimeoutException timeout = new TimeoutException("slow");
        IOException ioException = new IOException("socket closed");
        WebClientRequestException requestException = new WebClientRequestException(
                new IOException("network"),
                HttpMethod.POST,
                URI.create("https://api.github.com/graphql"),
                HttpHeaders.EMPTY
        );

        GitHubApiRetryableException timeoutResult = handler.handleTimeout(timeout, Duration.ofSeconds(20));
        GitHubApiRetryableException readTimeoutResult = handler.handleReadTimeout(ReadTimeoutException.INSTANCE);
        GitHubApiRetryableException ioResult = handler.handleIOException(ioException);
        GitHubApiRetryableException networkResult = handler.handleNetworkError(requestException);

        assertThat(timeoutResult.getErrorType()).isEqualTo(ErrorType.GITHUB_API_TIMEOUT);
        assertThat(timeoutResult.getCause()).isSameAs(timeout);
        assertThat(readTimeoutResult.getErrorType()).isEqualTo(ErrorType.GITHUB_API_TIMEOUT);
        assertThat(readTimeoutResult.getCause()).isSameAs(ReadTimeoutException.INSTANCE);
        assertThat(ioResult.getErrorType()).isEqualTo(ErrorType.GITHUB_API_ERROR);
        assertThat(ioResult.getCause()).isSameAs(ioException);
        assertThat(networkResult.getErrorType()).isEqualTo(ErrorType.GITHUB_API_ERROR);
        assertThat(networkResult.getCause()).isSameAs(requestException);
    }
}
