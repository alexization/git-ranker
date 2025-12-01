package com.gitranker.api.infrastructure.github;

import com.gitranker.api.global.exception.BusinessException;
import com.gitranker.api.global.exception.ErrorType;
import org.springframework.beans.factory.annotation.Value;
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
}
