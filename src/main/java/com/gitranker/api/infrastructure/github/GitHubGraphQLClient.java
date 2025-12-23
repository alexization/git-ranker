package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.exception.BusinessException;
import com.gitranker.api.global.exception.ErrorType;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import com.gitranker.api.infrastructure.github.dto.GitHubGraphQLRequest;
import com.gitranker.api.infrastructure.github.dto.GitHubUserInfoResponse;
import com.gitranker.api.infrastructure.github.util.GraphQLQueryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Component
public class GitHubGraphQLClient {
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
        GitHubGraphQLRequest request = GitHubGraphQLRequest.of(query);

        GitHubUserInfoResponse response = webClient.post()
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class).flatMap(body ->
                                Mono.error(new BusinessException(ErrorType.GITHUB_API_ERROR)))
                )
                .bodyToMono(GitHubUserInfoResponse.class)
                .block();

        if (response == null || response.data() == null || response.data().user() == null) {
            throw new BusinessException(ErrorType.GITHUB_USER_NOT_FOUND);
        }

        MdcUtils.setGithubApiCost(response.data().rateLimit().cost());

        return response;
    }

    public GitHubAllActivitiesResponse getAllActivities(String username, LocalDateTime githubJoinDate) {
        String query = GraphQLQueryBuilder.buildAllActivitiesQuery(username, githubJoinDate);
        GitHubGraphQLRequest request = GitHubGraphQLRequest.of(query);

        GitHubAllActivitiesResponse response = webClient.post()
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class).flatMap(body ->
                                Mono.error(new BusinessException(ErrorType.GITHUB_API_ERROR)))
                )
                .bodyToMono(GitHubAllActivitiesResponse.class)
                .block();

        if (response != null && response.hasErrors()) {
            throw new BusinessException(ErrorType.GITHUB_COLLECT_ACTIVITY_FAILED, "GitHub GraphQL Partial Error");
        }

        if (response == null || response.data() == null || response.data().getYearDataMap() == null) {
            throw new BusinessException(ErrorType.GITHUB_COLLECT_ACTIVITY_FAILED);
        }

        MdcUtils.setGithubApiCost(response.data().rateLimit().cost());

        return response;
    }
}
