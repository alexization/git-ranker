package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.exception.BusinessException;
import com.gitranker.api.global.exception.ErrorType;
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
            log.error("GitHub Token이 필수입니다. GraphQL API는 Token 없이 사용할 수 없습니다.");
            throw new IllegalArgumentException("GitHub Token is required for GraphQL API");
        }

        this.webClient = WebClient.builder()
                .baseUrl(graphqlUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public GitHubUserInfoResponse getUserInfo(String username) {
        log.info("사용자 기본 정보 조회 시작 - 사용자: {}", username);

        String query = GraphQLQueryBuilder.buildUserCreatedAtQuery(username);
        GitHubGraphQLRequest request = GitHubGraphQLRequest.of(query);

        return webClient.post()
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class).flatMap(body -> {
                            log.error("사용자 정보 조회 실패 - 사용자: {}, 응답: {}", username, body);

                            return Mono.error(new BusinessException(ErrorType.GITHUB_API_ERROR));
                        })
                )
                .bodyToMono(GitHubUserInfoResponse.class)
                .block();
    }

    public GitHubAllActivitiesResponse getAllActivities(String username, LocalDateTime githubJoinDate) {
        log.info("전체 활동 데이터 조회 시작 - 사용자: {}, 가입일: {}", username, githubJoinDate);

        String query = GraphQLQueryBuilder.buildAllActivitiesQuery(username, githubJoinDate);
        GitHubGraphQLRequest request = GitHubGraphQLRequest.of(query);

        return webClient.post()
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class).flatMap(body -> {
                            log.error("활동 데이터 조회 실패 - 사용자: {}, 응답: {}", username, body);

                            return Mono.error(new BusinessException(ErrorType.GITHUB_API_ERROR));
                        })
                )
                .bodyToMono(GitHubAllActivitiesResponse.class)
                .block();
    }
}
