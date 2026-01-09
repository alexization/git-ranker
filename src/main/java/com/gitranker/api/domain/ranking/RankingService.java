package com.gitranker.api.domain.ranking;

import com.gitranker.api.domain.ranking.dto.RankingList;
import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
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

    @Transactional(readOnly = true)
    public RankingList getRankingList(int page, Tier tier) {
        MdcUtils.setLogContext(LogCategory.DOMAIN, EventType.REQUEST);

        PageRequest pageable = PageRequest.of(page, DEFAULT_PAGE_SIZE);
        Page<User> userPage;

        if (tier == null) {
            userPage = userRepository.findAllByOrderByScoreValueDesc(pageable);
        } else {
            userPage = userRepository.findAllByRankInfoTierOrderByScoreValueDesc(tier, pageable);
        }

        List<User> userList = userPage.getContent();

        List<RankingList.UserInfo> userInfo = userList.stream()
                .map(user -> RankingList.UserInfo.from(user, user.getRanking()))
                .toList();

        Page<RankingList.UserInfo> rankingPage = new PageImpl<>(userInfo, pageable, userPage.getTotalElements());

        MdcUtils.setEventType(EventType.SUCCESS);
        log.info("랭킹 리스트 조회 - Page: {}, Tier: {}", page + 1, tier);

        return RankingList.from(rankingPage);
    }
}
