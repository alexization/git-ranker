package com.gitranker.api.domain.ranking;

import com.gitranker.api.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingRecalculationService {

    private static final Duration DEBOUNCE_DURATION = Duration.ofMinutes(5);
    private final UserRepository userRepository;
    private final RankingService rankingService;
    private volatile LocalDateTime lastRecalculationTime = null;

    @Transactional
    public synchronized boolean recalculateIfNeeded() {
        LocalDateTime now = LocalDateTime.now();

        if (shouldSkipRecalculation(now)) {
            return false;
        }

        userRepository.bulkUpdateRanking();
        lastRecalculationTime = now;
        rankingService.evictRankingCache();

        log.debug("랭킹 재산정 완료");

        return true;
    }

    private boolean shouldSkipRecalculation(LocalDateTime now) {
        if (lastRecalculationTime == null) {
            return false;
        }

        Duration elapsed = Duration.between(lastRecalculationTime, now);
        return elapsed.compareTo(DEBOUNCE_DURATION) < 0;
    }
}
