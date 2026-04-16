package com.gitranker.api.infrastructure.github.token;

import com.gitranker.api.global.error.exception.GitHubRateLimitExhaustedException;
import com.gitranker.api.global.error.message.ConfigurationMessages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubTokenPoolTest {

    private static final ZoneId APP_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("blank token config is rejected")
    void rejectsBlankTokenConfig() {
        assertThatThrownBy(() -> new GitHubTokenPool("   ", 100, APP_ZONE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ConfigurationMessages.GITHUB_TOKEN_NOT_CONFIGURED);
    }

    @Test
    @DisplayName("config with only empty token entries is rejected")
    void rejectsInvalidTokenEntries() {
        assertThatThrownBy(() -> new GitHubTokenPool(" , , ", 100, APP_ZONE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ConfigurationMessages.GITHUB_TOKEN_INVALID);
    }

    @Test
    @DisplayName("getToken returns the first available token")
    void returnsFirstAvailableToken() {
        GitHubTokenPool pool = new GitHubTokenPool("token-a, token-b", 100, APP_ZONE_ID);

        assertThat(pool.getToken()).isEqualTo("token-a");
    }

    @Test
    @DisplayName("getToken rotates to the next token when current token is below threshold")
    void rotatesToNextAvailableToken() {
        GitHubTokenPool pool = new GitHubTokenPool("token-a, token-b", 100, APP_ZONE_ID);

        pool.updateTokenState("token-a", 100, LocalDateTime.of(2099, 1, 1, 0, 0));

        assertThat(pool.getToken()).isEqualTo("token-b");
    }

    @Test
    @DisplayName("getToken throws exhausted exception with earliest reset time when all tokens are unavailable")
    void throwsWhenAllTokensExhausted() {
        GitHubTokenPool pool = new GitHubTokenPool("token-a, token-b", 100, APP_ZONE_ID);
        LocalDateTime firstReset = LocalDateTime.of(2026, 4, 16, 18, 0);
        LocalDateTime secondReset = LocalDateTime.of(2026, 4, 16, 19, 0);

        pool.updateTokenState("token-a", 10, firstReset);
        pool.updateTokenState("token-b", 20, secondReset);

        assertThatThrownBy(pool::getToken)
                .isInstanceOf(GitHubRateLimitExhaustedException.class)
                .satisfies(throwable ->
                        assertThat(((GitHubRateLimitExhaustedException) throwable).getEarliestResetAt()).isEqualTo(firstReset));
    }
}
