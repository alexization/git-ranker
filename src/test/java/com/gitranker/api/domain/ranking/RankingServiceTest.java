package com.gitranker.api.domain.ranking;

import com.gitranker.api.domain.ranking.dto.RankingList;
import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.vo.RankInfo;
import com.gitranker.api.domain.user.vo.Score;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static com.gitranker.api.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @InjectMocks
    private RankingService rankingService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("tier가 없으면 전체 랭킹 조회 쿼리를 사용한다")
    void getsOverallRankingWhenTierIsNull() {
        User user = user("alice");
        user.updateScore(Score.of(1500));
        user.updateRankInfo(RankInfo.of(1, 1.0, 1500));
        PageRequest pageRequest = PageRequest.of(0, 20);

        when(userRepository.findAllByOrderByScoreValueDesc(pageRequest))
                .thenReturn(new PageImpl<>(List.of(user), pageRequest, 1));

        RankingList rankingList = rankingService.getRankingList(0, null);

        assertThat(rankingList.rankings()).hasSize(1);
        assertThat(rankingList.rankings().getFirst().username()).isEqualTo("alice");
        assertThat(rankingList.pageInfo().currentPage()).isZero();
        assertThat(rankingList.pageInfo().totalElements()).isEqualTo(1);
        verify(userRepository).findAllByOrderByScoreValueDesc(pageRequest);
    }

    @Test
    @DisplayName("tier가 있으면 해당 tier 전용 랭킹 조회 쿼리를 사용한다")
    void getsTierRankingWhenTierIsProvided() {
        User user = user("diamond-user");
        user.updateScore(Score.of(3200));
        user.updateRankInfo(RankInfo.of(2, 10.0, 3200));
        PageRequest pageRequest = PageRequest.of(1, 20);

        when(userRepository.findAllByRankInfoTierOrderByScoreValueDesc(Tier.DIAMOND, pageRequest))
                .thenReturn(new PageImpl<>(List.of(user), pageRequest, 21));

        RankingList rankingList = rankingService.getRankingList(1, Tier.DIAMOND);

        assertThat(rankingList.rankings()).hasSize(1);
        assertThat(rankingList.rankings().getFirst().tier()).isEqualTo(Tier.DIAMOND);
        assertThat(rankingList.pageInfo().currentPage()).isEqualTo(1);
        assertThat(rankingList.pageInfo().totalElements()).isEqualTo(21);
        verify(userRepository).findAllByRankInfoTierOrderByScoreValueDesc(Tier.DIAMOND, pageRequest);
    }
}
