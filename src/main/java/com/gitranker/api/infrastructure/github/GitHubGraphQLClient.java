package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.error.*;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import com.gitranker.api.global.error.exception.GitHubRateLimitException;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import com.gitranker.api.infrastructure.github.dto.GitHubGraphQLRequest;
import com.gitranker.api.infrastructure.github.dto.GitHubUserInfoResponse;
import com.gitranker.api.infrastructure.github.util.GraphQLQueryBuilder;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    public GitHubGraphQLClient(
            @Value("${github.api.graphql-url}") String graphqlUrl,
            @Value("${github.api.token}") String token,
            GraphQLQueryBuilder queryBuilder,
            ZoneId appZoneId
    ) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("GitHub Token is required for GraphQL API");
        }

        this.webClient = WebClient.builder()
                .baseUrl(graphqlUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.queryBuilder = queryBuilder;
        this.appZoneId = appZoneId;
    }

    public GitHubUserInfoResponse getUserInfo(String username) {
        String query = queryBuilder.buildUserCreatedAtQuery(username);

        GitHubUserInfoResponse response = executeQuery(query, GitHubUserInfoResponse.class);

        if (response.data().rateLimit() != null) {
            recordRateLimitInfo(response.data().rateLimit());

            checkRateLimitSafety(
                    response.data().rateLimit().remaining(),
                    response.data().rateLimit().resetAt()
            );
        }

        return response;
    }

    public GitHubAllActivitiesResponse getActivitiesForYear(String username, int year) {
        String query = queryBuilder.buildBatchQuery(username, year);

        GitHubAllActivitiesResponse response = executeQuery(query, GitHubAllActivitiesResponse.class);

        if (response.data().rateLimit() != null) {
            recordRateLimitInfo(response.data().rateLimit());

            checkRateLimitSafety(
                    response.data().rateLimit().remaining(),
                    response.data().rateLimit().resetAt()
            );
        }

        return response;
    }

    public GitHubAllActivitiesResponse getAllActivities(String username, LocalDateTime githubJoinDate) {
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
                .flatMap(query -> executeQueryReactive(query, GitHubAllActivitiesResponse.class))
                .sequential()
                .reduce(GitHubAllActivitiesResponse.empty(), (acc, current) -> {
                    acc.merge(current);
                    return acc;
                })
                .block();

        if (aggregatedResponse == null || aggregatedResponse.data() == null) {
            throw new GitHubApiNonRetryableException(ErrorType.GITHUB_COLLECT_ACTIVITY_FAILED);
        }

        if (aggregatedResponse.data().rateLimit() != null) {
            recordRateLimitInfo(aggregatedResponse.data().rateLimit());
        }

        return aggregatedResponse;
    }

    private void checkRateLimitSafety(int remaining, LocalDateTime resetAt) {
        if (remaining < SAFE_REMAINING_THRESHOLD) {
            log.warn("[GitHub API] Rate Limit Check Failed. Remaining: {}, ResetAt: {}", remaining, resetAt);

            throw new GitHubRateLimitException(resetAt);
        }
    }

    private <T> Mono<T> executeQueryReactive(String query, Class<T> responseType) {
        GitHubGraphQLRequest request = GitHubGraphQLRequest.of(query);

        return webClient.post()
                .bodyValue(request)
                .exchangeToMono(response -> {
                    if (response.statusCode().value() == 403 || response.statusCode().value() == 429) {
                        LocalDateTime resetAt = parseResetTime(response.headers());

                        return Mono.error(new GitHubRateLimitException(resetAt));
                    }

                    if (response.statusCode().is4xxClientError()) {
                        return Mono.error(new GitHubApiRetryableException(ErrorType.GITHUB_API_CLIENT_ERROR, "Status: " + response.statusCode()));
                    }

                    if (response.statusCode().is5xxServerError()) {
                        return Mono.error(new GitHubApiRetryableException(ErrorType.GITHUB_API_SERVER_ERROR, "Status: " + response.statusCode()));
                    }

                    return response.bodyToMono(responseType)
                            .map(body -> {
                                if (body instanceof GitHubAllActivitiesResponse r && r.hasErrors()) {
                                    handleGraphQLErrors(r.errors());
                                }
                                return body;
                            });
                })
                .timeout(API_TIMEOUT)
                .onErrorMap(TimeoutException.class, e -> new GitHubApiRetryableException(ErrorType.GITHUB_API_TIMEOUT, e))
                .onErrorMap(ReadTimeoutException.class, e -> new GitHubApiRetryableException(ErrorType.GITHUB_API_TIMEOUT, e))
                .onErrorMap(IOException.class, e -> new GitHubApiRetryableException(ErrorType.GITHUB_API_ERROR, e))
                .doOnError(e -> {
                    if (e instanceof WebClientRequestException) {
                        log.warn("[GitHub API] Network Error: {}", e.getMessage());

                        throw new GitHubApiRetryableException(ErrorType.GITHUB_API_ERROR, e);
                    }
                });
    }

    private <T> T executeQuery(String query, Class<T> responseType) {
        try {
            return executeQueryReactive(query, responseType)
                    .block();
        } catch (GitHubApiRetryableException | GitHubApiNonRetryableException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GitHub API] Unexpected Error", e);
            throw new BusinessException(ErrorType.GITHUB_API_ERROR, e.getMessage());
        }
    }

    private LocalDateTime parseResetTime(ClientResponse.Headers headers) {
        return headers.header("x-ratelimit-reset").stream()
                .findFirst()
                .map(Long::parseLong)
                .map(epoch -> LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), appZoneId))
                .orElseGet(() -> LocalDateTime.now(appZoneId).plusMinutes(60));
    }

    private void handleGraphQLErrors(List<Object> errors) {
        log.warn("[GitHub API] GraphQL Error: {}", errors);

        String errorString = errors.toString();
        if (errorString.contains("Could not resolve to a User")) {
            throw new GitHubApiNonRetryableException(ErrorType.GITHUB_USER_NOT_FOUND);
        }

        throw new GitHubApiRetryableException(ErrorType.GITHUB_PARTIAL_ERROR);
    }

    private void recordRateLimitInfo(GitHubAllActivitiesResponse.RateLimit rateLimit) {
        MdcUtils.setGithubApiCost(rateLimit.cost());
        MdcUtils.setGithubApiRemaining(rateLimit.remaining());
        MdcUtils.setGithubApiResetAt(formatToKST(rateLimit.resetAt()));
    }

    private void recordRateLimitInfo(GitHubUserInfoResponse.RateLimit rateLimit) {
        MdcUtils.setGithubApiCost(rateLimit.cost());
        MdcUtils.setGithubApiRemaining(rateLimit.remaining());
        MdcUtils.setGithubApiResetAt(formatToKST(rateLimit.resetAt()));
    }

    private String formatToKST(LocalDateTime UTCDateTime) {
        if (UTCDateTime == null) return null;

        return UTCDateTime.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(appZoneId)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
