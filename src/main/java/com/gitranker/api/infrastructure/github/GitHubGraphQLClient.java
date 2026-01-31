package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.auth.AuthConstants;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import com.gitranker.api.global.error.exception.GitHubRateLimitException;
import com.gitranker.api.global.error.message.ConfigurationMessages;
import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import com.gitranker.api.infrastructure.github.dto.GitHubGraphQLRequest;
import com.gitranker.api.infrastructure.github.dto.GitHubRateLimitInfo;
import com.gitranker.api.infrastructure.github.dto.GitHubUserInfoResponse;
import com.gitranker.api.infrastructure.github.token.GitHubTokenPool;
import com.gitranker.api.infrastructure.github.util.GraphQLQueryBuilder;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

@Slf4j
@Component
public class GitHubGraphQLClient {
    private static final Duration API_TIMEOUT = Duration.ofSeconds(20);
    private static final int CONCURRENCY_LIMIT = 5;
    private static final int SAFE_REMAINING_THRESHOLD = 50;

    private final WebClient webClient;
    private final GraphQLQueryBuilder queryBuilder;
    private final ZoneId appZoneId;
    private final GitHubApiMetrics apiMetrics;
    private final GitHubTokenPool tokenPool;
    private final GitHubApiErrorHandler errorHandler;

    public GitHubGraphQLClient(
            @Value("${github.api.graphql-url}") String graphqlUrl,
            GraphQLQueryBuilder queryBuilder,
            ZoneId appZoneId,
            WebClient.Builder webClientBuilder,
            GitHubApiMetrics apiMetrics,
            GitHubTokenPool tokenPool,
            GitHubApiErrorHandler errorHandler
    ) {
        this.webClient = webClientBuilder
                .baseUrl(graphqlUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.queryBuilder = queryBuilder;
        this.appZoneId = appZoneId;
        this.apiMetrics = apiMetrics;
        this.tokenPool = tokenPool;
        this.errorHandler = errorHandler;
    }

    private void validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException(ConfigurationMessages.GITHUB_ACCESS_TOKEN_REQUIRED);
        }
    }

    public GitHubUserInfoResponse getUserInfo(String accessToken, String username) {
        String query = queryBuilder.buildUserCreatedAtQuery(username);

        GitHubUserInfoResponse response = executeQuery(accessToken, query, GitHubUserInfoResponse.class);

        if (response.data().rateLimit() != null) {
            recordRateLimitInfo(accessToken, response.data().rateLimit());

            checkRateLimitSafety(
                    response.data().rateLimit().remaining(),
                    response.data().rateLimit().resetAt()
            );
        }

        return response;
    }

    public GitHubAllActivitiesResponse getAllActivities(String accessToken, String username, LocalDateTime githubJoinDate) {
        validateAccessToken(accessToken);

        int joinYear = githubJoinDate.getYear();
        int currentYear = LocalDateTime.now(appZoneId).getYear();

        Flux<String> queries = Flux.concat(
                Flux.just(queryBuilder.buildMergedPRBlock(username)),
                Flux.fromStream(IntStream.rangeClosed(joinYear, currentYear).boxed())
                        .map(year -> queryBuilder.buildYearlyContributionQuery(username, year, githubJoinDate))
        );

        GitHubAllActivitiesResponse aggregatedResponse = queries
                .parallel(CONCURRENCY_LIMIT)
                .runOn(Schedulers.boundedElastic())
                .flatMap(query -> executeQueryReactive(accessToken, query, GitHubAllActivitiesResponse.class))
                .sequential()
                .reduce(GitHubAllActivitiesResponse.empty(), (acc, current) -> {
                    acc.merge(current);
                    return acc;
                })
                .block();

        if (aggregatedResponse == null || aggregatedResponse.data() == null) {
            log.error("활동 데이터 수집 실패 - 사용자: {}", username);

            throw new GitHubApiNonRetryableException(ErrorType.GITHUB_COLLECT_ACTIVITY_FAILED);
        }

        if (aggregatedResponse.data().rateLimit() != null) {
            recordRateLimitInfo(accessToken, aggregatedResponse.data().rateLimit());
        }

        return aggregatedResponse;
    }

    public GitHubAllActivitiesResponse getActivitiesForYear(String accessToken, String username, int year) {
        String query = queryBuilder.buildBatchQuery(username, year);

        GitHubAllActivitiesResponse response = executeQuery(accessToken, query, GitHubAllActivitiesResponse.class);

        if (response.data().rateLimit() != null) {
            recordRateLimitInfo(accessToken, response.data().rateLimit());

            checkRateLimitSafety(
                    response.data().rateLimit().remaining(),
                    response.data().rateLimit().resetAt()
            );
        }

        return response;
    }

    private void checkRateLimitSafety(int remaining, LocalDateTime resetAt) {
        if (remaining < SAFE_REMAINING_THRESHOLD) {
            LogContext.event(Event.RATE_LIMIT_WARNING)
                    .with("remaining", remaining)
                    .with("threshold", SAFE_REMAINING_THRESHOLD)
                    .with("reset_at", resetAt.toString())
                    .warn();

            apiMetrics.recordRateLimitExceeded();

            throw new GitHubRateLimitException(resetAt);
        }
    }

    private <T> Mono<T> executeQueryReactive(String accessToken, String query, Class<T> responseType) {
        GitHubGraphQLRequest request = GitHubGraphQLRequest.of(query);

        return webClient.post()
                .header("Authorization", AuthConstants.BEARER_PREFIX + accessToken)
                .bodyValue(request)
                .exchangeToMono(response -> {
                    RuntimeException httpError = errorHandler.handleHttpStatus(response);
                    if (httpError != null) {
                        return Mono.error(httpError);
                    }

                    return response.bodyToMono(responseType)
                            .map(body -> {
                                if (body instanceof GitHubAllActivitiesResponse r && r.hasErrors()) {
                                    errorHandler.handleGraphQLErrors(r.errors());
                                }
                                return body;
                            });
                })
                .timeout(API_TIMEOUT)
                .onErrorMap(TimeoutException.class, e -> errorHandler.handleTimeout(e, API_TIMEOUT))
                .onErrorMap(ReadTimeoutException.class, errorHandler::handleReadTimeout)
                .onErrorMap(IOException.class, errorHandler::handleIOException)
                .doOnError(e -> {
                    if (e instanceof WebClientRequestException wce) {
                        throw errorHandler.handleNetworkError(wce);
                    }
                });
    }

    private <T> T executeQuery(String accessToken, String query, Class<T> responseType) {
        validateAccessToken(accessToken);

        try {
            return executeQueryReactive(accessToken, query, responseType)
                    .block();
        } catch (GitHubApiRetryableException | GitHubApiNonRetryableException e) {
            throw e;
        } catch (Exception e) {
            log.error("예기치 않은 API 에러 발생", e);

            throw new BusinessException(ErrorType.GITHUB_API_ERROR, e.getMessage());
        }
    }

    private void recordRateLimitInfo(String accessToken, GitHubRateLimitInfo rateLimit) {
        apiMetrics.recordRateLimit(rateLimit.cost(), rateLimit.remaining(), rateLimit.resetAt());

        tokenPool.updateTokenState(accessToken, rateLimit.remaining(), rateLimit.resetAt());
    }
}
