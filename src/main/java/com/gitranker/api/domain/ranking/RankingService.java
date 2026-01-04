package com.gitranker.api.domain.ranking;

import com.gitranker.api.domain.ranking.dto.RankingInfo;
import com.gitranker.api.domain.ranking.dto.RankingList;
import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.aop.LogExecutionTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private final UserRepository userRepository;
    private final TierCalculator tierCalculator;

    @Transactional(readOnly = true)
    @LogExecutionTime
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

    @Transactional(readOnly = true)
    @LogExecutionTime
    public RankingList getRankingList(int page, Tier tier) {
        log.info("[Domain Event] 랭킹 리스트 조회 - Page: {}, Tier: {}", page + 1, tier);

        PageRequest pageable = PageRequest.of(page, DEFAULT_PAGE_SIZE);
        Page<User> userPage;

        if (tier == null) {
            userPage = userRepository.findAllByOrderByTotalScoreDesc(pageable);
        } else {
            userPage = userRepository.findAllByTierOrderByTotalScoreDesc(tier, pageable);
        }

        List<User> userList = userPage.getContent();

        List<RankingList.UserInfo> userInfo = userList.stream()
                .map(user -> RankingList.UserInfo.from(user, user.getRanking()))
                .toList();

        Page<RankingList.UserInfo> rankingPage = new PageImpl<>(userInfo, pageable, userPage.getTotalElements());

        return RankingList.from(rankingPage);
    }
}
