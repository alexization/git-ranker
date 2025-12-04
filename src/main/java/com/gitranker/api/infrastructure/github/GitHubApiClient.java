package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.exception.BusinessException;
import com.gitranker.api.global.exception.ErrorType;
import com.gitranker.api.infrastructure.github.dto.GitHubCommitSearchItem;
import com.gitranker.api.infrastructure.github.dto.GitHubIssueSearchItem;
import com.gitranker.api.infrastructure.github.dto.GitHubSearchResonse;
import com.gitranker.api.infrastructure.github.dto.GitHubUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class GitHubApiClient {
    private final RestClient restClient;

    public GitHubApiClient(@Value("${github.api.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public GitHubUserResponse getUser(String username) {
        try {
            return restClient.get()
                    .uri("/users/{username}", username)
                    .retrieve()
                    .body(GitHubUserResponse.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BusinessException(ErrorType.USER_NOT_FOUND);
            }
            throw new BusinessException(ErrorType.GITHUB_API_ERROR);
        }
    }

    public GitHubSearchResonse<GitHubCommitSearchItem> searchCommits(
            String query, int page, int perPage
    ) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/commits")
                            .queryParam("q", query)
                            .queryParam("page", page)
                            .queryParam("per_page", perPage)
                            .build())
                    .header("Accept", "application/vnd.github.cloak-preview")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (HttpClientErrorException e) {
            throw new BusinessException(ErrorType.GITHUB_API_ERROR);
        }
    }

    public GitHubSearchResonse<GitHubIssueSearchItem> searchIssues(
            String query, int page, int perPage
    ) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/issues")
                            .queryParam("q", query)
                            .queryParam("page", page)
                            .queryParam("per_page", perPage)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (HttpClientErrorException e) {
            throw new BusinessException(ErrorType.GITHUB_API_ERROR);
        }
    }
}
