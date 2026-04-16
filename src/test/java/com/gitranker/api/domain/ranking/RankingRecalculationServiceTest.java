package com.gitranker.api.domain.ranking;

import com.gitranker.api.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RankingRecalculationServiceTest {

    @InjectMocks
    private RankingRecalculationService rankingRecalculationService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RankingService rankingService;

    @Test
    @DisplayName("첫 호출이면 랭킹을 재산정하고 캐시를 비운다")
    void recalculatesOnFirstCall() {
        boolean recalculated = rankingRecalculationService.recalculateIfNeeded();

        assertThat(recalculated).isTrue();
        verify(userRepository).bulkUpdateRanking();
        verify(rankingService).evictRankingCache();
    }

    @Test
    @DisplayName("디바운스 구간 안의 연속 호출은 건너뛴다")
    void skipsRecalculationWithinDebounceWindow() {
        rankingRecalculationService.recalculateIfNeeded();

        boolean recalculated = rankingRecalculationService.recalculateIfNeeded();

        assertThat(recalculated).isFalse();
        verify(userRepository, times(1)).bulkUpdateRanking();
        verify(rankingService, times(1)).evictRankingCache();
    }

    @Test
    @DisplayName("마지막 재산정 시각이 오래되었으면 다시 재산정한다")
    void recalculatesAfterDebounceWindowExpires() {
        ReflectionTestUtils.setField(
                rankingRecalculationService,
                "lastRecalculationTime",
                LocalDateTime.of(2020, 1, 1, 0, 0)
        );

        boolean recalculated = rankingRecalculationService.recalculateIfNeeded();

        assertThat(recalculated).isTrue();
        verify(userRepository).bulkUpdateRanking();
        verify(rankingService).evictRankingCache();
    }
}
