package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.exception.BusinessException;
import com.gitranker.api.global.exception.ErrorType;
import com.gitranker.api.global.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.exception.GitHubApiRetryableException;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import com.gitranker.api.infrastructure.github.dto.GitHubGraphQLRequest;
import com.gitranker.api.infrastructure.github.dto.GitHubUserInfoResponse;
import com.gitranker.api.infrastructure.github.util.GraphQLQueryBuilder;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class GitHubGraphQLClient {
    private static final Duration API_TIMEOUT = Duration.ofSeconds(30);
    private final WebClient webClient;

    public GitHubGraphQLClient(
            @Value("${github.api.graphql-url}") String graphqlUrl,
            @Value("${github.api.token}") String token
    ) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("GitHub Token is required for GraphQL API");
        }

        this.webClient = WebClient.builder()
                .baseUrl(graphqlUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public GitHubUserInfoResponse getUserInfo(String username) {
        String query = GraphQLQueryBuilder.buildUserCreatedAtQuery(username);

        return executeQuery(query, GitHubUserInfoResponse.class);
    }

    public GitHubAllActivitiesResponse getAllActivities(String username, LocalDateTime githubJoinDate) {
        String query = GraphQLQueryBuilder.buildAllActivitiesQuery(username, githubJoinDate);
        GitHubAllActivitiesResponse response = executeQuery(query, GitHubAllActivitiesResponse.class);

        if (response != null && response.hasErrors()) {
            log.error("[GitHub API] GraphQL Error: {}", response.errors());
            throw new GitHubApiNonRetryableException(ErrorType.GITHUB_PARTIAL_ERROR, response.errors().toString());
        }

        if (response == null || response.data() == null || response.data().getYearDataMap() == null) {
            throw new GitHubApiNonRetryableException(ErrorType.GITHUB_COLLECT_ACTIVITY_FAILED, "Invalid Response Data");
        }

        MdcUtils.setGithubApiCost(response.data().rateLimit().cost());
        return response;
    }

    private <T> T executeQuery(String query, Class<T> responseType) {
        GitHubGraphQLRequest request = GitHubGraphQLRequest.of(query);

        try {
            return webClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, res ->
                            Mono.error(new GitHubApiRetryableException(ErrorType.GITHUB_API_SERVER_ERROR, "Status: " + res.statusCode())))
                    .onStatus(HttpStatusCode::is4xxClientError, res ->
                            Mono.error(new GitHubApiNonRetryableException(ErrorType.GITHUB_API_CLIENT_ERROR, "Status: " + res.statusCode())))
                    .bodyToMono(responseType)
                    .timeout(API_TIMEOUT)
                    .onErrorMap(TimeoutException.class, e ->
                            new GitHubApiRetryableException(ErrorType.GITHUB_API_TIMEOUT, e))
                    .onErrorMap(ReadTimeoutException.class, e ->
                            new GitHubApiRetryableException(ErrorType.GITHUB_API_TIMEOUT, e))
                    .block();
        } catch (WebClientRequestException e) {
            log.warn("[GitHub API] Network Error: {}", e.getMessage());
            throw new GitHubApiRetryableException(ErrorType.GITHUB_API_TIMEOUT, e);

        } catch (GitHubApiRetryableException | GitHubApiNonRetryableException e) {
            throw e;

        } catch (Exception e) {
            log.error("[GitHub API] Unexpected Error", e);
            throw new BusinessException(ErrorType.GITHUB_API_ERROR, e.getMessage());
        }
    }
}
