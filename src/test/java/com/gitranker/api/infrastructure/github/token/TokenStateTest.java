package com.gitranker.api.infrastructure.github.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TokenStateTest {

    private static final Instant FUTURE_RESET_AT = Instant.parse("2099-01-01T00:00:00Z");
    private static final Instant PAST_RESET_AT = Instant.parse("2000-01-01T00:00:00Z");

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

        state.update(50, FUTURE_RESET_AT);

        assertThat(state.getRemaining()).isEqualTo(50);
        assertThat(state.getResetAt()).isEqualTo(FUTURE_RESET_AT);
        assertThat(state.isAvailable(100)).isFalse();
    }

    @Test
    @DisplayName("availability check restores default limit after reset time passes")
    void restoresLimitAfterResetTime() {
        TokenState state = new TokenState("token-a");
        state.update(0, PAST_RESET_AT);

        boolean available = state.isAvailable(100);

        assertThat(available).isTrue();
        assertThat(state.getRemaining()).isEqualTo(TokenState.DEFAULT_LIMIT);
    }
}
