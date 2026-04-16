package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import com.gitranker.api.global.error.exception.GitHubRateLimitException;
import com.gitranker.api.global.error.message.ConfigurationMessages;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import com.gitranker.api.infrastructure.github.dto.GitHubUserInfoResponse;
import com.gitranker.api.infrastructure.github.token.GitHubTokenPool;
import com.gitranker.api.infrastructure.github.util.GraphQLQueryBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubGraphQLClientTest {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private GraphQLQueryBuilder queryBuilder;

    @Mock
    private GitHubApiMetrics apiMetrics;

    @Mock
    private GitHubTokenPool tokenPool;

    @Mock
    private GitHubApiErrorHandler errorHandler;

    @Test
    @DisplayName("getUserInfo records rate-limit info and returns parsed response")
    void getsUserInfo() {
        LocalDateTime resetAt = LocalDateTime.of(2026, 4, 16, 18, 0);
        when(queryBuilder.buildUserCreatedAtQuery("alice")).thenReturn("user-query");

        GitHubGraphQLClient client = client(request -> Mono.just(jsonResponse("""
                {
                  "data": {
                    "rateLimit": {"limit":5000,"cost":3,"remaining":120,"resetAt":"2026-04-16T18:00:00"},
                    "user": {
                      "id":"node-1",
                      "createdAt":"2020-01-01T00:00:00Z",
                      "login":"alice",
                      "avatarUrl":"avatar"
                    }
                  }
                }
                """)));

        GitHubUserInfoResponse response = client.getUserInfo("token-a", "alice");

        assertThat(response.getLogin()).isEqualTo("alice");
        assertThat(response.getNodeId()).isEqualTo("node-1");
        verify(apiMetrics).recordRateLimit(3, 120, resetAt);
        verify(tokenPool).updateTokenState("token-a", 120, resetAt);
    }

    @Test
    @DisplayName("getUserInfo throws rate-limit exception when remaining budget is below threshold")
    void throwsWhenRateLimitGetsUnsafe() {
        LocalDateTime resetAt = LocalDateTime.of(2026, 4, 16, 18, 0);
        when(queryBuilder.buildUserCreatedAtQuery("alice")).thenReturn("user-query");

        GitHubGraphQLClient client = client(request -> Mono.just(jsonResponse("""
                {
                  "data": {
                    "rateLimit": {"limit":5000,"cost":2,"remaining":10,"resetAt":"2026-04-16T18:00:00"},
                    "user": {
                      "id":"node-1",
                      "createdAt":"2020-01-01T00:00:00Z",
                      "login":"alice",
                      "avatarUrl":"avatar"
                    }
                  }
                }
                """)));

        assertThatThrownBy(() -> client.getUserInfo("token-a", "alice"))
                .isInstanceOf(GitHubRateLimitException.class)
                .satisfies(throwable -> assertThat(((GitHubRateLimitException) throwable).getResetAt()).isEqualTo(resetAt));

        verify(apiMetrics).recordRateLimit(2, 10, resetAt);
        verify(tokenPool).updateTokenState("token-a", 10, resetAt);
        verify(apiMetrics).recordRateLimitExceeded();
    }

    @Test
    @DisplayName("getAllActivities merges merged-pr block and yearly contribution response")
    void aggregatesAllActivities() {
        int currentYear = LocalDate.now(APP_ZONE).getYear();
        LocalDateTime joinedAt = LocalDateTime.of(currentYear, 1, 2, 10, 0);
        LocalDateTime mergedResetAt = LocalDateTime.of(2026, 4, 16, 18, 30);
        LocalDateTime resetAt = LocalDateTime.of(2026, 4, 16, 19, 0);
        when(queryBuilder.buildMergedPRBlock("alice")).thenReturn("merged-query");
        when(queryBuilder.buildYearlyContributionQuery("alice", currentYear, joinedAt)).thenReturn("year-query");

        GitHubGraphQLClient client = client(request -> {
            String body = requestBody(request);
            if (body.contains("merged-query")) {
                return Mono.just(jsonResponse(String.format("""
                        {
                          "data": {
                            "rateLimit": {"limit":5000,"cost":1,"remaining":200,"resetAt":"%s"},
                            "mergedPRs": {"issueCount":7}
                          }
                        }
                        """, mergedResetAt)));
            }
            if (body.contains("year-query")) {
                return Mono.just(jsonResponse(String.format("""
                        {
                          "data": {
                            "rateLimit": {"limit":5000,"cost":2,"remaining":199,"resetAt":"%s"},
                            "year%d": {
                              "contributionsCollection": {
                                "totalCommitContributions":3,
                                "totalIssueContributions":4,
                                "totalPullRequestContributions":5,
                                "totalPullRequestReviewContributions":6
                              }
                            }
                          }
                        }
                        """, resetAt, currentYear)));
            }
            return Mono.error(new IllegalStateException("Unexpected request body: " + body));
        });

        GitHubAllActivitiesResponse response = client.getAllActivities("token-a", "alice", joinedAt);

        assertThat(response.getCommitCount()).isEqualTo(3);
        assertThat(response.getIssueCount()).isEqualTo(4);
        assertThat(response.getPRCount()).isEqualTo(5);
        assertThat(response.getMergedPRCount()).isEqualTo(7);
        assertThat(response.getReviewCount()).isEqualTo(6);

        ArgumentCaptor<Integer> remainingCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<LocalDateTime> resetCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(apiMetrics).recordRateLimit(org.mockito.ArgumentMatchers.eq(3), remainingCaptor.capture(), resetCaptor.capture());
        verify(tokenPool).updateTokenState("token-a", remainingCaptor.getValue(), resetCaptor.getValue());
        assertThat(remainingCaptor.getValue()).isIn(199, 200);
        assertThat(resetCaptor.getValue()).isIn(resetAt, mergedResetAt);
    }

    @Test
    @DisplayName("getActivitiesForYear delegates GraphQL errors to the error handler")
    void delegatesGraphQLErrors() {
        GitHubApiRetryableException mapped = new GitHubApiRetryableException(ErrorType.GITHUB_PARTIAL_ERROR);
        when(queryBuilder.buildBatchQuery("alice", 2026)).thenReturn("batch-query");
        doThrow(mapped).when(errorHandler).handleGraphQLErrors(anyList());

        GitHubGraphQLClient client = client(request -> Mono.just(jsonResponse("""
                {
                  "data": {},
                  "errors": ["partial error"]
                }
                """)));

        assertThatThrownBy(() -> client.getActivitiesForYear("token-a", "alice", 2026))
                .isSameAs(mapped);

        verify(errorHandler).handleGraphQLErrors(anyList());
        verify(apiMetrics, never()).recordRateLimit(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("network errors are remapped through the error handler")
    void remapsNetworkErrors() {
        WebClientRequestException requestException = new WebClientRequestException(
                new IOException("network"),
                HttpMethod.POST,
                URI.create("https://api.github.com/graphql"),
                HttpHeaders.EMPTY
        );
        GitHubApiRetryableException mapped = new GitHubApiRetryableException(ErrorType.GITHUB_API_ERROR, "network");
        when(queryBuilder.buildBatchQuery("alice", 2026)).thenReturn("batch-query");
        when(errorHandler.handleNetworkError(requestException)).thenReturn(mapped);

        GitHubGraphQLClient client = client(request -> Mono.error(requestException));

        assertThatThrownBy(() -> client.getActivitiesForYear("token-a", "alice", 2026))
                .isSameAs(mapped);
    }

    @Test
    @DisplayName("unexpected client errors are wrapped with business exception")
    void wrapsUnexpectedErrors() {
        when(queryBuilder.buildUserCreatedAtQuery("alice")).thenReturn("user-query");
        GitHubGraphQLClient client = client(request -> Mono.error(new IllegalStateException("boom")));

        assertThatThrownBy(() -> client.getUserInfo("token-a", "alice"))
                .isInstanceOf(BusinessException.class)
                .satisfies(throwable -> {
                    BusinessException exception = (BusinessException) throwable;
                    assertThat(exception.getErrorType()).isEqualTo(ErrorType.GITHUB_API_ERROR);
                    assertThat(exception.getData()).isEqualTo("boom");
                });
    }

    @Test
    @DisplayName("blank access token is rejected before GraphQL execution")
    void rejectsBlankAccessToken() {
        GitHubGraphQLClient client = client(request -> Mono.error(new AssertionError("should not execute")));

        assertThatThrownBy(() -> client.getUserInfo("   ", "alice"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ConfigurationMessages.GITHUB_ACCESS_TOKEN_REQUIRED);
    }

    private GitHubGraphQLClient client(ExchangeFunction exchangeFunction) {
        return new GitHubGraphQLClient(
                "https://api.github.com/graphql",
                queryBuilder,
                APP_ZONE,
                WebClient.builder().exchangeFunction(exchangeFunction),
                apiMetrics,
                tokenPool,
                errorHandler
        );
    }

    private ClientResponse jsonResponse(String json) {
        return ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build();
    }

    private String requestBody(ClientRequest request) {
        MockClientHttpRequest httpRequest = new MockClientHttpRequest(request.method(), request.url());
        request.body().insert(httpRequest, new BodyInserter.Context() {
            @Override
            public List<HttpMessageWriter<?>> messageWriters() {
                return ExchangeStrategies.withDefaults().messageWriters();
            }

            @Override
            public Optional<ServerHttpRequest> serverRequest() {
                return Optional.empty();
            }

            @Override
            public java.util.Map<String, Object> hints() {
                return Collections.emptyMap();
            }
        }).block();
        return httpRequest.getBodyAsString().block();
    }
}
