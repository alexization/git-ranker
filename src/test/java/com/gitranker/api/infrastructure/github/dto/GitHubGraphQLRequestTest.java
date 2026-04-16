package com.gitranker.api.infrastructure.github.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubGraphQLRequestTest {

    @Test
    @DisplayName("factory method stores GraphQL query string")
    void createsRequest() {
        GitHubGraphQLRequest request = GitHubGraphQLRequest.of("{ viewer { login } }");

        assertThat(request.query()).isEqualTo("{ viewer { login } }");
    }
}
