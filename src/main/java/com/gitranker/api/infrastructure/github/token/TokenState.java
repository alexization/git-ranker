package com.gitranker.api.infrastructure.github.token;

import lombok.Getter;

import java.time.Instant;

@Getter
public class TokenState {

    private static final int DEFAULT_LIMIT = 5000;

    private final String value;
    private volatile int remaining;
    private volatile Instant resetAt;

    public TokenState(String value) {
        this.value = value;
        this.remaining = DEFAULT_LIMIT;
        this.resetAt = Instant.now().plusSeconds(3600);
    }

    public boolean isAvailable(int threshold) {
        if (Instant.now().isAfter(resetAt)) {
            this.remaining = DEFAULT_LIMIT;
        }

        return remaining > threshold;
    }

    public void update(int remaining, Instant resetAt) {
        this.remaining = remaining;
        this.resetAt = resetAt;
    }
}
