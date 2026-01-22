package com.gitranker.api.infrastructure.github.token;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class GitHubTokenPool {

    private final List<TokenState> tokens;
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final int threshold;
    private final ZoneId appZoneId;

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
            throw new IllegalArgumentException("GitHub 토큰이 설정되지 않았습니다.");
        }

        List<TokenState> parsedTokens = Arrays.stream(tokensConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(TokenState::new)
                .toList();

        if (parsedTokens.isEmpty()) {
            throw new IllegalArgumentException("유효한 GitHub API 토큰이 없습니다.");
        }

        return parsedTokens;
    }

    public String getToken() {
        int startIndex = currentIndex.get();
        int size = tokens.size();

        for (int i = 0; i < size; i++) {
            int idx = (startIndex + i) % size;
            TokenState token = tokens.get(idx);

            if (token.isAvailable(threshold)) {
                if (idx != startIndex) {
                    currentIndex.set(idx);
                    log.info("{}번 토큰 {}번 토큰으로 전환", startIndex, idx);
                }
                return token.getValue();
            }
        }

        LocalDateTime earliestResetAt = findEarliestResetAt();
        log.warn("모든 토큰 소진 - 복구 예정 시간: {}", earliestResetAt);

        throw new GitHubRateLimitExhaustedException(earliestResetAt);
    }

    private LocalDateTime findEarliestResetAt() {
        return tokens.stream()
                .map(TokenState::getResetAt)
                .min(Comparator.naturalOrder())
                .map(instant -> LocalDateTime.ofInstant(instant, appZoneId))
                .orElse(LocalDateTime.now(appZoneId).plusHours(1));
    }
}
