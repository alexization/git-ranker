package com.gitranker.api.infrastructure.github.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TokenStateTest {

    @Test
    @DisplayName("new token state starts with full limit and is available")
    void initializesWithDefaultLimit() {
        TokenState state = new TokenState("token-a");

        assertThat(state.getValue()).isEqualTo("token-a");
        assertThat(state.getRemaining()).isEqualTo(TokenState.DEFAULT_LIMIT);
        assertThat(state.isAvailable(100)).isTrue();
    }

    @Test
    @DisplayName("update changes remaining budget and reset time")
    void updatesState() {
        TokenState state = new TokenState("token-a");
        Instant resetAt = Instant.now().plusSeconds(300);

        state.update(50, resetAt);

        assertThat(state.getRemaining()).isEqualTo(50);
        assertThat(state.getResetAt()).isEqualTo(resetAt);
        assertThat(state.isAvailable(100)).isFalse();
    }

    @Test
    @DisplayName("availability check restores default limit after reset time passes")
    void restoresLimitAfterResetTime() {
        TokenState state = new TokenState("token-a");
        state.update(0, Instant.now().minusSeconds(1));

        boolean available = state.isAvailable(100);

        assertThat(available).isTrue();
        assertThat(state.getRemaining()).isEqualTo(TokenState.DEFAULT_LIMIT);
    }
}
