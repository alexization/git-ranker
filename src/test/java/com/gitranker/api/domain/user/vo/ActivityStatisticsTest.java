package com.gitranker.api.domain.user.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityStatisticsTest {

    @Test
    @DisplayName("활동 통계는 문서화된 가중치로 점수를 계산한다")
    void calculatesScoreWithConfiguredWeights() {
        ActivityStatistics statistics = ActivityStatistics.of(3, 2, 1, 5, 4);

        assertThat(statistics.calculateScore()).isEqualTo(Score.of(72));
    }

    @Test
    @DisplayName("이전 통계보다 감소한 항목은 diff가 음수가 된다")
    void calculatesNegativeDiffWhenActivityDecreases() {
        ActivityStatistics current = ActivityStatistics.of(10, 5, 3, 2, 1);
        ActivityStatistics previous = ActivityStatistics.of(12, 3, 5, 1, 4);

        ActivityStatistics diff = current.calculateDiff(previous);

        assertThat(diff).isEqualTo(ActivityStatistics.of(-2, 2, -2, 1, -3));
    }

    @Test
    @DisplayName("merge와 totalActivityCount는 모든 항목을 합산한다")
    void mergesAllActivityCounts() {
        ActivityStatistics merged = ActivityStatistics.of(1, 2, 3, 4, 5)
                .merge(ActivityStatistics.of(5, 4, 3, 2, 1));

        assertThat(merged).isEqualTo(ActivityStatistics.of(6, 6, 6, 6, 6));
        assertThat(merged.totalActivityCount()).isEqualTo(30);
        assertThat(merged.hasActivity()).isTrue();
    }

    @Test
    @DisplayName("empty 통계는 활동이 없고 문자열 표현도 고정된다")
    void emptyStatisticsHasNoActivity() {
        ActivityStatistics empty = ActivityStatistics.empty();

        assertThat(empty.hasActivity()).isFalse();
        assertThat(empty.totalActivityCount()).isZero();
        assertThat(empty.toString()).isEqualTo("ActivityStatistics{commits=0, issues=0, prs=0, merged=0, reviews=0}");
    }
}
