package com.gitranker.api.domain.ranking;

import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
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
    private volatile LocalDateTime lastRecalculationTime = null;

    @Transactional
    public synchronized boolean recalculateIfNeeded() {
        MdcUtils.setLogContext(LogCategory.DOMAIN, EventType.REQUEST);

        LocalDateTime now = LocalDateTime.now();

        if (shouldSkipRecalculation(now)) {
            MdcUtils.setEventType(EventType.SKIP);
            return false;
        }

        userRepository.bulkUpdateRanking();
        lastRecalculationTime = now;

        MdcUtils.setEventType(EventType.SUCCESS);
        log.info("랭킹 재산정 완료");

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
