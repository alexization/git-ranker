package com.gitranker.api.infrastructure.github.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubNodeUserResponseTest {

    @Test
    @DisplayName("node user response exposes mapped fields when a user exists")
    void exposesMappedFields() {
        GitHubNodeUserResponse response = new GitHubNodeUserResponse(
                new GitHubNodeUserResponse.Data(
                        new GitHubNodeUserResponse.Node("node-1", "alice", "alice@example.com", "avatar"),
                        null
                )
        );

        assertThat(response.hasUser()).isTrue();
        assertThat(response.getLogin()).isEqualTo("alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getAvatarUrl()).isEqualTo("avatar");
    }

    @Test
    @DisplayName("node user response returns null fields when node user is missing")
    void returnsNullFieldsWhenUserMissing() {
        GitHubNodeUserResponse response = new GitHubNodeUserResponse(
                new GitHubNodeUserResponse.Data(
                        new GitHubNodeUserResponse.Node("node-1", null, "alice@example.com", "avatar"),
                        null
                )
        );

        assertThat(response.hasUser()).isFalse();
        assertThat(response.getLogin()).isNull();
        assertThat(response.getEmail()).isNull();
        assertThat(response.getAvatarUrl()).isNull();
    }
}
