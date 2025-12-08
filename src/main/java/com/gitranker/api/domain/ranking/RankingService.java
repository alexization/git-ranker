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
        long totalUserCount = userRepository.count();

        return calculateRanking(userScore, totalUserCount);
    }

    @Transactional(readOnly = true)
    public RankingInfo calculateRanking(int userScore, long totalUserCount) {
        long higherScoreCount = userRepository.countByTotalScoreGreaterThan(userScore);

        int ranking = (int) higherScoreCount + 1;
        double percentile = (double) ranking / totalUserCount * 100.0;

        Tier tier = tierCalculator.calculateTier(percentile);

        return new RankingInfo(ranking, percentile, tier);
    }
}
