package com.gitranker.api.batch.processor;

import com.gitranker.api.domain.ranking.TierCalculator;
import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingRecalculationProcessor implements ItemProcessor<User, User> {
    private final UserRepository userRepository;
    private final TierCalculator tierCalculator;

    private long totalUserCount;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.totalUserCount = userRepository.count();
        log.info("순위 재산정 시작 - 전체 사용자 : {} 명", totalUserCount);
    }

    @Override
    public User process(User user) throws Exception {
        long higherScoreCount = userRepository.countByTotalScoreGreaterThan(user.getTotalScore());
        int ranking = (int) higherScoreCount + 1;

        double percentile = (double) ranking / totalUserCount * 100.0;

        Tier tier = tierCalculator.calculateTier(percentile);

        user.updateRankInfo(ranking, percentile, tier);

        return user;
    }
}
