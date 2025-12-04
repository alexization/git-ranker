package com.gitranker.api.domain.ranking;

import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {
    private final UserRepository userRepository;
    private final TierCalculator tierCalculator;

    @Transactional(readOnly = true)
    public RankingInfo calculateRankingForNewUser(int userScore) {
        log.debug("신규 사용자 순위 및 티어 계산 시작 - 점수 : {}", userScore);

        long higherScoreCount = userRepository.countByTotalScoreGreaterThan(userScore);
        int ranking = (int) higherScoreCount + 1;

        long totalUserCount = userRepository.count();

        double percentile = (double) ranking / totalUserCount * 100.0;

        Tier tier = tierCalculator.calculateTier(percentile);

        RankingInfo rankingInfo = new RankingInfo(ranking, percentile, tier);
        log.info("신규 사용자 순위 및 티어 계산 완료 - {}", rankingInfo);

        return rankingInfo;
    }
}
