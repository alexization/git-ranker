package com.gitranker.api.global.error.exception;

import com.gitranker.api.global.error.ErrorType;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
public class GitHubRateLimitExhaustedException extends GitHubApiRetryableException {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final LocalDateTime earliestResetAt;

    public GitHubRateLimitExhaustedException(LocalDateTime earliestResetAt) {
        super(
                ErrorType.GITHUB_RATE_LIMIT_EXHAUSTED,
                String.format("모든 토큰 소진. %s 이후 재시도 가능", earliestResetAt.format(FORMATTER))
        );
        this.earliestResetAt = earliestResetAt;
    }

    public String getTimeUntilReset() {
        long seconds = getSecondsUntilReset();
        if (seconds <= 0) {
            return "곧 복구됩니다.";
        }

        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%d분 %d초 후 복구", minutes, remainingSeconds);
        }
        return String.format("%d초 후 복구", remainingSeconds);
    }

    public long getSecondsUntilReset() {
        return Duration.between(LocalDateTime.now(), earliestResetAt).getSeconds();
    }
}
