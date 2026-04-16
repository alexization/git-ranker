package com.gitranker.api.infrastructure.github.dto;

import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubUserInfoResponseTest {

    @Test
    @DisplayName("user info response parses created-at and exposes profile fields")
    void parsesUserInfoFields() {
        GitHubUserInfoResponse response = new GitHubUserInfoResponse(
                new GitHubUserInfoResponse.Data(
                        new GitHubUserInfoResponse.User("node-1", "2020-01-01T00:00:00Z", "alice", "avatar"),
                        new GitHubUserInfoResponse.RateLimit(5000, 1, 100, LocalDateTime.of(2026, 4, 16, 18, 0))
                )
        );

        assertThat(response.getGitHubCreatedAt()).isEqualTo(LocalDateTime.of(2020, 1, 1, 0, 0));
        assertThat(response.getLogin()).isEqualTo("alice");
        assertThat(response.getAvatarUrl()).isEqualTo("avatar");
        assertThat(response.getNodeId()).isEqualTo("node-1");
    }

    @Test
    @DisplayName("getNodeId throws business exception when user id is missing")
    void throwsWhenNodeIdMissing() {
        GitHubUserInfoResponse response = new GitHubUserInfoResponse(
                new GitHubUserInfoResponse.Data(
                        new GitHubUserInfoResponse.User(null, "2020-01-01T00:00:00Z", "alice", "avatar"),
                        null
                )
        );

        assertThatThrownBy(response::getNodeId)
                .isInstanceOf(BusinessException.class)
                .satisfies(throwable ->
                        assertThat(((BusinessException) throwable).getErrorType()).isEqualTo(ErrorType.GITHUB_USER_NOT_FOUND));
    }
}
