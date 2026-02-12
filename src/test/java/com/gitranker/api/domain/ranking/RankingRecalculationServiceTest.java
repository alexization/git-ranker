package com.gitranker.api.domain.ranking;

import com.gitranker.api.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingRecalculationServiceTest {

    @InjectMocks
    private RankingRecalculationService rankingRecalculationService;

    @Mock private UserRepository userRepository;
    @Mock private RankingService rankingService;

    @Test
    @DisplayName("첫 호출 시 랭킹을 재산정한다")
    void should_recalculate_when_calledFirstTime() {
        boolean result = rankingRecalculationService.recalculateIfNeeded();

        assertThat(result).isTrue();
        verify(userRepository).bulkUpdateRanking();
        verify(rankingService).evictRankingCache();
    }

    @Test
    @DisplayName("연속 호출 시 디바운스로 두 번째 호출을 건너뛴다")
    void should_skipRecalculation_when_calledImmediatelyAgain() {
        rankingRecalculationService.recalculateIfNeeded(); // 첫 번째: 실행
        boolean result = rankingRecalculationService.recalculateIfNeeded(); // 두 번째: 건너뜀

        assertThat(result).isFalse();
        verify(userRepository, times(1)).bulkUpdateRanking(); // 1번만 호출
    }

    @Test
    @DisplayName("재산정 완료 후 랭킹 캐시를 무효화한다")
    void should_evictCache_when_recalculationCompleted() {
        rankingRecalculationService.recalculateIfNeeded();

        verify(rankingService).evictRankingCache();
    }
}
