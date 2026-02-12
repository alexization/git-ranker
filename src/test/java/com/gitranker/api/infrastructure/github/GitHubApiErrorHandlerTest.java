package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import com.gitranker.api.global.error.exception.GitHubRateLimitException;
import io.netty.handler.timeout.ReadTimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubApiErrorHandlerTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private GitHubApiMetrics apiMetrics;

    @InjectMocks
    private GitHubApiErrorHandler errorHandler;

    /**
     * @InjectMocks는 appZoneId(ZoneId)를 주입하지 못하므로 직접 생성한다.
     */
    private GitHubApiErrorHandler createHandler() {
        return new GitHubApiErrorHandler(ZONE, apiMetrics);
    }

    @Nested
    @DisplayName("handleHttpStatus")
    class HandleHttpStatus {

        @Test
        @DisplayName("403 응답이면 GitHubRateLimitException을 반환한다")
        void should_returnRateLimitException_when_status403() {
            GitHubApiErrorHandler handler = createHandler();
            long resetEpoch = Instant.now().plusSeconds(3600).getEpochSecond();

            ClientResponse response = ClientResponse.create(HttpStatus.FORBIDDEN)
                    .header("x-ratelimit-reset", String.valueOf(resetEpoch))
                    .build();

            RuntimeException result = handler.handleHttpStatus(response);

            assertThat(result).isInstanceOf(GitHubRateLimitException.class);
            GitHubRateLimitException rateLimitEx = (GitHubRateLimitException) result;
            assertThat(rateLimitEx.getResetAt()).isNotNull();
            verify(apiMetrics).recordRateLimitExceeded();
        }

        @Test
        @DisplayName("429 응답이면 GitHubRateLimitException을 반환한다")
        void should_returnRateLimitException_when_status429() {
            GitHubApiErrorHandler handler = createHandler();
            long resetEpoch = Instant.now().plusSeconds(3600).getEpochSecond();

            ClientResponse response = ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS)
                    .header("x-ratelimit-reset", String.valueOf(resetEpoch))
                    .build();

            RuntimeException result = handler.handleHttpStatus(response);

            assertThat(result).isInstanceOf(GitHubRateLimitException.class);
            verify(apiMetrics).recordRateLimitExceeded();
        }

        @Test
        @DisplayName("403 응답에 reset 헤더가 없으면 현재 시간 + 60분으로 fallback한다")
        void should_fallbackResetTime_when_noResetHeader() {
            GitHubApiErrorHandler handler = createHandler();
            LocalDateTime before = LocalDateTime.now(ZONE).plusMinutes(59);

            ClientResponse response = ClientResponse.create(HttpStatus.FORBIDDEN).build();

            RuntimeException result = handler.handleHttpStatus(response);

            assertThat(result).isInstanceOf(GitHubRateLimitException.class);
            GitHubRateLimitException rateLimitEx = (GitHubRateLimitException) result;
            assertThat(rateLimitEx.getResetAt()).isAfter(before);
        }

        @Test
        @DisplayName("4xx 응답(403 제외)이면 CLIENT_ERROR 타입의 RetryableException을 반환한다")
        void should_returnClientError_when_status4xx() {
            GitHubApiErrorHandler handler = createHandler();

            ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST).build();

            RuntimeException result = handler.handleHttpStatus(response);

            assertThat(result).isInstanceOf(GitHubApiRetryableException.class);
            GitHubApiRetryableException retryableEx = (GitHubApiRetryableException) result;
            assertThat(retryableEx.getErrorType()).isEqualTo(ErrorType.GITHUB_API_CLIENT_ERROR);
            verify(apiMetrics).recordFailure();
        }

        @Test
        @DisplayName("5xx 응답이면 SERVER_ERROR 타입의 RetryableException을 반환한다")
        void should_returnServerError_when_status5xx() {
            GitHubApiErrorHandler handler = createHandler();

            ClientResponse response = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build();

            RuntimeException result = handler.handleHttpStatus(response);

            assertThat(result).isInstanceOf(GitHubApiRetryableException.class);
            GitHubApiRetryableException retryableEx = (GitHubApiRetryableException) result;
            assertThat(retryableEx.getErrorType()).isEqualTo(ErrorType.GITHUB_API_SERVER_ERROR);
            verify(apiMetrics).recordFailure();
        }

        @Test
        @DisplayName("2xx 응답이면 null을 반환한다")
        void should_returnNull_when_status2xx() {
            GitHubApiErrorHandler handler = createHandler();

            ClientResponse response = ClientResponse.create(HttpStatus.OK).build();

            RuntimeException result = handler.handleHttpStatus(response);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("handleGraphQLErrors")
    class HandleGraphQLErrors {

        @Test
        @DisplayName("에러 리스트가 null이면 예외를 던지지 않는다")
        void should_doNothing_when_errorsNull() {
            GitHubApiErrorHandler handler = createHandler();

            handler.handleGraphQLErrors(null);
            // 예외 없이 정상 종료
        }

        @Test
        @DisplayName("에러 리스트가 비어있으면 예외를 던지지 않는다")
        void should_doNothing_when_errorsEmpty() {
            GitHubApiErrorHandler handler = createHandler();

            handler.handleGraphQLErrors(Collections.emptyList());
            // 예외 없이 정상 종료
        }

        @Test
        @DisplayName("사용자를 찾을 수 없는 에러면 NonRetryableException을 던진다")
        void should_throwNonRetryable_when_userNotFound() {
            GitHubApiErrorHandler handler = createHandler();

            List<Object> errors = List.of("Could not resolve to a User with the login of 'unknown'");

            assertThatThrownBy(() -> handler.handleGraphQLErrors(errors))
                    .isInstanceOf(GitHubApiNonRetryableException.class)
                    .extracting(e -> ((GitHubApiNonRetryableException) e).getErrorType())
                    .isEqualTo(ErrorType.GITHUB_USER_NOT_FOUND);
        }

        @Test
        @DisplayName("기타 GraphQL 에러면 PARTIAL_ERROR 타입의 RetryableException을 던진다")
        void should_throwRetryable_when_otherGraphQLError() {
            GitHubApiErrorHandler handler = createHandler();

            List<Object> errors = List.of("Something went wrong");

            assertThatThrownBy(() -> handler.handleGraphQLErrors(errors))
                    .isInstanceOf(GitHubApiRetryableException.class)
                    .extracting(e -> ((GitHubApiRetryableException) e).getErrorType())
                    .isEqualTo(ErrorType.GITHUB_PARTIAL_ERROR);
        }
    }

    @Nested
    @DisplayName("handleTimeout / handleReadTimeout / handleIOException / handleNetworkError")
    class HandleOtherErrors {

        @Test
        @DisplayName("TimeoutException이면 TIMEOUT 타입의 RetryableException을 반환한다")
        void should_returnTimeout_when_timeoutException() {
            GitHubApiErrorHandler handler = createHandler();

            GitHubApiRetryableException result = handler.handleTimeout(
                    new TimeoutException("timed out"), Duration.ofSeconds(30));

            assertThat(result.getErrorType()).isEqualTo(ErrorType.GITHUB_API_TIMEOUT);
            assertThat(result.getCause()).isInstanceOf(TimeoutException.class);
        }

        @Test
        @DisplayName("ReadTimeoutException이면 TIMEOUT 타입의 RetryableException을 반환한다")
        void should_returnTimeout_when_readTimeoutException() {
            GitHubApiErrorHandler handler = createHandler();

            GitHubApiRetryableException result = handler.handleReadTimeout(
                    ReadTimeoutException.INSTANCE);

            assertThat(result.getErrorType()).isEqualTo(ErrorType.GITHUB_API_TIMEOUT);
        }

        @Test
        @DisplayName("IOException이면 API_ERROR 타입의 RetryableException을 반환한다")
        void should_returnApiError_when_ioException() {
            GitHubApiErrorHandler handler = createHandler();

            GitHubApiRetryableException result = handler.handleIOException(
                    new IOException("connection reset"));

            assertThat(result.getErrorType()).isEqualTo(ErrorType.GITHUB_API_ERROR);
            assertThat(result.getCause()).isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("WebClientRequestException이면 API_ERROR 타입의 RetryableException을 반환한다")
        void should_returnApiError_when_networkError() {
            GitHubApiErrorHandler handler = createHandler();

            WebClientRequestException networkEx = new WebClientRequestException(
                    new IOException("connection refused"),
                    org.springframework.http.HttpMethod.POST,
                    URI.create("https://api.github.com/graphql"),
                    org.springframework.http.HttpHeaders.EMPTY);

            GitHubApiRetryableException result = handler.handleNetworkError(networkEx);

            assertThat(result.getErrorType()).isEqualTo(ErrorType.GITHUB_API_ERROR);
            assertThat(result.getCause()).isInstanceOf(WebClientRequestException.class);
        }
    }
}
