package com.gitranker.api.domain.user.vo;

import com.gitranker.api.domain.user.Tier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RankInfoTest {

    @Test
    @DisplayName("전체 사용자가 0명이면 초기 랭크 정보로 시작한다")
    void returnsInitialRankInfoWhenTotalUserCountIsZero() {
        RankInfo rankInfo = RankInfo.calculate(10, 0, 3000);

        assertThat(rankInfo).isEqualTo(RankInfo.initial());
    }

    @ParameterizedTest
    @CsvSource({
            "0.5, 2000, CHALLENGER",
            "5.0, 2000, MASTER",
            "12.0, 2000, DIAMOND",
            "25.0, 2000, EMERALD",
            "45.0, 2000, PLATINUM",
            "46.0, 2000, GOLD"
    })
    @DisplayName("고득점 구간에서는 percentile 경계값으로 상위 티어가 결정된다")
    void calculatesHighTierByPercentile(double percentile, int score, Tier expectedTier) {
        assertThat(RankInfo.of(1, percentile, score).getTier()).isEqualTo(expectedTier);
    }

    @ParameterizedTest
    @CsvSource({
            "499, IRON",
            "500, BRONZE",
            "999, BRONZE",
            "1000, SILVER",
            "1499, SILVER",
            "1500, GOLD",
            "1999, GOLD"
    })
    @DisplayName("고득점 최소치 미만에서는 절대 점수 기준 티어를 사용한다")
    void calculatesScoreBasedTierBelowHighTierThreshold(int score, Tier expectedTier) {
        assertThat(RankInfo.of(3, 99.0, score).getTier()).isEqualTo(expectedTier);
    }

    @Test
    @DisplayName("유효하지 않은 랭킹과 백분위는 예외를 발생시킨다")
    void validatesRankingAndPercentile() {
        assertThatThrownBy(() -> RankInfo.of(-1, 10.0, 500))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1");
        assertThatThrownBy(() -> RankInfo.of(1, 100.01, 500))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100.01");
    }

    @Test
    @DisplayName("승급 여부와 상위 퍼센트 판정 helper를 제공한다")
    void exposesPromotionAndTopPercentHelpers() {
        RankInfo promoted = RankInfo.of(2, 4.0, 2200);
        RankInfo previous = RankInfo.of(20, 50.0, 1600);

        assertThat(promoted.isTierPromoted(previous)).isTrue();
        assertThat(promoted.isTopPercent(5.0)).isTrue();
        assertThat(promoted.toString()).contains("Rank 2").contains("MASTER");
    }
}
