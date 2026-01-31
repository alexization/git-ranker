package com.gitranker.api.infrastructure.github.token;

import com.gitranker.api.global.error.exception.GitHubRateLimitExhaustedException;
import com.gitranker.api.global.error.message.ConfigurationMessages;
import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class GitHubTokenPool {

    private final ReentrantLock tokenLock = new ReentrantLock();
    private final List<TokenState> tokens;
    private final int threshold;
    private final ZoneId appZoneId;
    private int currentIndex = 0;

    public GitHubTokenPool(
            @Value("${github.api.tokens}") String tokensConfig,
            @Value("${github.api.threshold}") int threshold,
            ZoneId appZoneId
    ) {
        this.tokens = parseTokens(tokensConfig);
        this.threshold = threshold;
        this.appZoneId = appZoneId;
    }

    private List<TokenState> parseTokens(String tokensConfig) {
        if (tokensConfig == null || tokensConfig.isBlank()) {
            throw new IllegalStateException(ConfigurationMessages.GITHUB_TOKEN_NOT_CONFIGURED);
        }

        List<TokenState> parsedTokens = Arrays.stream(tokensConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(TokenState::new)
                .toList();

        if (parsedTokens.isEmpty()) {
            throw new IllegalStateException(ConfigurationMessages.GITHUB_TOKEN_INVALID);
        }

        return parsedTokens;
    }

    public String getToken() {
        tokenLock.lock();
        try {
            int startIndex = currentIndex;
            int size = tokens.size();

            for (int i = 0; i < size; i++) {
                int idx = (startIndex + i) % size;
                TokenState token = tokens.get(idx);

                if (token.isAvailable(threshold)) {
                    if (idx != startIndex) {
                        log.info("{}번 토큰에서 {}번 토큰으로 전환", startIndex, idx);
                        currentIndex = idx;
                    }
                    return token.getValue();
                }
            }

            LocalDateTime earliestResetAt = findEarliestResetAt();
            log.warn("모든 토큰 소진 - 복구 예정 시간: {}", earliestResetAt);

            throw new GitHubRateLimitExhaustedException(earliestResetAt);
        } finally {
            tokenLock.unlock();
        }
    }

    private LocalDateTime findEarliestResetAt() {
        return tokens.stream()
                .map(TokenState::getResetAt)
                .min(Comparator.naturalOrder())
                .map(instant -> LocalDateTime.ofInstant(instant, appZoneId))
                .orElse(LocalDateTime.now(appZoneId).plusHours(1));
    }

    public void updateTokenState(String tokenValue, int remaining, LocalDateTime resetAt) {
        tokenLock.lock();
        try {
            tokens.stream()
                    .filter(t -> t.getValue().equals(tokenValue))
                    .findFirst()
                    .ifPresent(token -> {
                        Instant resetInstant = resetAt.atZone(appZoneId).toInstant();
                        token.update(remaining, resetInstant);

                        if (remaining <= threshold) {
                            LogContext.event(Event.RATE_LIMIT_WARNING)
                                    .with("remaining", remaining)
                                    .with("threshold", threshold)
                                    .with("reset_at", resetAt.toString())
                                    .warn();
                        }
                    });
        } finally {
            tokenLock.unlock();
        }
    }
}
