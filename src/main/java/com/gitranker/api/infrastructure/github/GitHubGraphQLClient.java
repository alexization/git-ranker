package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import com.gitranker.api.global.error.exception.GitHubRateLimitException;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.global.util.TimeUtils;
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
    private final TimeUtils timeUtils;

    public GitHubGraphQLClient(
            @Value("${github.api.graphql-url}") String graphqlUrl,
            @Value("${github.api.token}") String token,
            GraphQLQueryBuilder queryBuilder,
            ZoneId appZoneId,
            TimeUtils timeUtils
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
        this.timeUtils = timeUtils;
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
            MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
            MdcUtils.setError(ErrorType.GITHUB_COLLECT_ACTIVITY_FAILED.name(), "응답 데이터 없음");
            log.error("활동 데이터 수집 실패 - 사용자: {}", username);

            throw new GitHubApiNonRetryableException(ErrorType.GITHUB_COLLECT_ACTIVITY_FAILED);
        }

        if (aggregatedResponse.data().rateLimit() != null) {
            recordRateLimitInfo(aggregatedResponse.data().rateLimit());
        }

        return aggregatedResponse;
    }

    private void checkRateLimitSafety(int remaining, LocalDateTime resetAt) {
        if (remaining < SAFE_REMAINING_THRESHOLD) {
            MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
            log.warn("Rate Limit 임계값 도달 - Remaining: {}, ResetAt: {}", remaining, resetAt);

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

                        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.THRESHOLD);
                        log.warn("Rate Limit 초과 - Status: {}, ResetAt: {}", response.statusCode().value(), resetAt);

                        return Mono.error(new GitHubRateLimitException(resetAt));
                    }

                    if (response.statusCode().is4xxClientError()) {
                        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
                        log.error("API 클라이언트 에러 - Status: {}", response.statusCode());

                        return Mono.error(new GitHubApiRetryableException(ErrorType.GITHUB_API_CLIENT_ERROR, "Status: " + response.statusCode()));
                    }

                    if (response.statusCode().is5xxServerError()) {
                        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
                        log.error("API 서버 에러 - Status: {}", response.statusCode());

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
                .onErrorMap(TimeoutException.class, e -> {
                    MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
                    MdcUtils.setError("TimeoutException", "API 호출 타임아웃: " + API_TIMEOUT.getSeconds());

                    log.warn("API 타임아웃 발생 - Timeout: {}초", API_TIMEOUT.getSeconds());

                    return new GitHubApiRetryableException(ErrorType.GITHUB_API_TIMEOUT, e);
                })
                .onErrorMap(ReadTimeoutException.class, e -> {
                    MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
                    MdcUtils.setError("ReadTimeoutException", "읽기 타임아웃");

                    log.warn("읽기 타임아웃 발생");

                    return new GitHubApiRetryableException(ErrorType.GITHUB_API_TIMEOUT, e);
                })
                .onErrorMap(IOException.class, e -> {
                    MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
                    MdcUtils.setError("IOException", e.getMessage());

                    log.warn("IO 에러 발생 - {}", e.getMessage());

                    return new GitHubApiRetryableException(ErrorType.GITHUB_API_ERROR, e);
                })
                .doOnError(e -> {
                    if (e instanceof WebClientRequestException) {
                        MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
                        MdcUtils.setError("NetworkError", e.getMessage());

                        log.warn("네트워크 에러 발생 - {}", e.getMessage());

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
            MdcUtils.setLogContext(LogCategory.EXTERNAL_API, EventType.FAILURE);
            MdcUtils.setError(e.getClass().getSimpleName(), e.getMessage());

            log.error("예기치 않은 API 에러 발생", e);

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

    private void recordRateLimitInfo(GitHubAllActivitiesResponse.RateLimit rateLimit) {
        MdcUtils.setGithubApiCost(rateLimit.cost());
        MdcUtils.setGithubApiRemaining(rateLimit.remaining());
        MdcUtils.setGithubApiResetAt(timeUtils.formatForLog(rateLimit.resetAt()));
    }

    private void recordRateLimitInfo(GitHubUserInfoResponse.RateLimit rateLimit) {
        MdcUtils.setGithubApiCost(rateLimit.cost());
        MdcUtils.setGithubApiRemaining(rateLimit.remaining());
        MdcUtils.setGithubApiResetAt(timeUtils.formatForLog(rateLimit.resetAt()));
    }
}
