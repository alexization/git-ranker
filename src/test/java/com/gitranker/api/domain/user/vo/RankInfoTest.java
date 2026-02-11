package com.gitranker.api.domain.user.vo;

import com.gitranker.api.domain.user.Tier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RankInfoTest {

    @Nested
    @DisplayName("절대 티어 (점수 기반)")
    class AbsoluteTier {

        @ParameterizedTest
        @DisplayName("점수 구간에 따라 올바른 절대 티어를 반환한다")
        @CsvSource({
                "0,    IRON",
                "499,  IRON",
                "500,  BRONZE",
                "999,  BRONZE",
                "1000, SILVER",
                "1499, SILVER",
                "1500, GOLD",
                "1999, GOLD"
        })
        void should_returnCorrectAbsoluteTier(int score, Tier expected) {
            // percentile 50% -> 상대 티어 조건 미충족
            RankInfo rankInfo = RankInfo.of(1, 50.0, score);

            assertThat(rankInfo.getTier()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("상대 티어 (백분위 기반, 2000점 이상)")
    class RelativeTier {

        @ParameterizedTest
        @DisplayName("2000점 이상에서 백분위에 따라 상대 티어를 반환한다")
        @CsvSource({
                "0.5,  CHALLENGER",
                "1.0,  CHALLENGER",
                "1.1,  MASTER",
                "5.0,  MASTER",
                "5.1,  DIAMOND",
                "12.0, DIAMOND",
                "12.1, EMERALD",
                "25.0, EMERALD",
                "25.1, PLATINUM",
                "45.0, PLATINUM"
        })
        void should_returnCorrectRelativeTier_when_scoreAbove2000(double percentile, Tier expected) {
            RankInfo rankInfo = RankInfo.of(1, percentile, 2000);

            assertThat(rankInfo.getTier()).isEqualTo(expected);
        }

        @Test
        @DisplayName("2000점 이상이어도 백분위가 45%를 넘으면 GOLD로 떨어진다")
        void should_fallbackToGold_when_percentileAbove45() {
            RankInfo rankInfo = RankInfo.of(1, 45.1, 2000);

            assertThat(rankInfo.getTier()).isEqualTo(Tier.GOLD);
        }

        @Test
        @DisplayName("1999점이면 top 1%여도 상대 티어가 아닌 GOLD를 반환한다")
        void should_returnGold_when_scoreBelowThresholdEvenIfTopPercent() {
            RankInfo rankInfo = RankInfo.of(1, 0.5, 1999);

            assertThat(rankInfo.getTier()).isEqualTo(Tier.GOLD);
        }
    }

    @Nested
    @DisplayName("calculate")
    class Calculate {

        @Test
        @DisplayName("전체 사용자 수가 0이면 초기값을 반환한다")
        void should_returnInitial_when_noUsers() {
            RankInfo rankInfo = RankInfo.calculate(0, 0, 500);

            assertThat(rankInfo.getRanking()).isZero();
            assertThat(rankInfo.getPercentile()).isEqualTo(100.0);
            assertThat(rankInfo.getTier()).isEqualTo(Tier.IRON);
        }

        @Test
        @DisplayName("higherScoreCount로부터 정확한 랭킹을 계산한다")
        void should_calculateRanking_from_higherScoreCount() {
            // 나보다 높은 사람 9명, 전체 100명 -> 랭킹 10위, 백분위 10%
            RankInfo rankInfo = RankInfo.calculate(9, 100, 2000);

            assertThat(rankInfo.getRanking()).isEqualTo(10);
            assertThat(rankInfo.getPercentile()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("1등은 백분위 계산이 정확하다")
        void should_calculateFirstPlace_correctly() {
            // 나보다 높은 사람 0명, 전체 100명 -> 1위, 1%
            RankInfo rankInfo = RankInfo.calculate(0, 100, 3000);

            assertThat(rankInfo.getRanking()).isEqualTo(1);
            assertThat(rankInfo.getPercentile()).isEqualTo(1.0);
            assertThat(rankInfo.getTier()).isEqualTo(Tier.CHALLENGER);
        }

        @Test
        @DisplayName("유일한 사용자는 1등이지만 백분위 100%라 상대 티어를 받지 못한다")
        void should_beFirstRank_but_fallbackToAbsoluteTier_when_onlyOneUser() {
            RankInfo rankInfo = RankInfo.calculate(0, 1, 2500);

            assertThat(rankInfo.getRanking()).isEqualTo(1);
            assertThat(rankInfo.getPercentile()).isEqualTo(100.0);
            // 2500점이지만 percentile 100% > 45%이므로 상대 티어(PLATINUM~CHALLENGER)가 아닌 GOLD
            assertThat(rankInfo.getTier()).isEqualTo(Tier.GOLD);
        }
    }

    @Nested
    @DisplayName("initial")
    class Initial {

        @Test
        @DisplayName("초기 RankInfo는 0위, 100%, IRON이다")
        void should_returnDefaultValues() {
            RankInfo initial = RankInfo.initial();

            assertThat(initial.getRanking()).isZero();
            assertThat(initial.getPercentile()).isEqualTo(100.0);
            assertThat(initial.getTier()).isEqualTo(Tier.IRON);
        }
    }

    @Nested
    @DisplayName("티어 승급 판별")
    class TierPromotion {

        @Test
        @DisplayName("티어가 올라갔으면 승급으로 판별한다")
        void should_detectPromotion_when_tierImproved() {
            RankInfo previous = RankInfo.of(10, 50.0, 400);  // IRON
            RankInfo current = RankInfo.of(5, 30.0, 600);    // BRONZE

            assertThat(current.isTierPromoted(previous)).isTrue();
        }

        @Test
        @DisplayName("티어가 같으면 승급이 아니다")
        void should_notDetectPromotion_when_sameTier() {
            RankInfo previous = RankInfo.of(10, 50.0, 600);  // BRONZE
            RankInfo current = RankInfo.of(5, 30.0, 700);    // BRONZE

            assertThat(current.isTierPromoted(previous)).isFalse();
        }
    }

    @Nested
    @DisplayName("유효성 검증")
    class Validation {

        @Test
        @DisplayName("랭킹이 음수이면 예외가 발생한다")
        void should_throwException_when_negativeRanking() {
            assertThatThrownBy(() -> RankInfo.of(-1, 50.0, 500))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("백분위가 0 미만이면 예외가 발생한다")
        void should_throwException_when_percentileBelowZero() {
            assertThatThrownBy(() -> RankInfo.of(1, -0.1, 500))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("백분위가 100 초과이면 예외가 발생한다")
        void should_throwException_when_percentileAbove100() {
            assertThatThrownBy(() -> RankInfo.of(1, 100.1, 500))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("isTopPercent")
    class IsTopPercent {

        @Test
        @DisplayName("백분위가 임계값 이하이면 true를 반환한다")
        void should_returnTrue_when_withinThreshold() {
            RankInfo rankInfo = RankInfo.of(1, 5.0, 2000);

            assertThat(rankInfo.isTopPercent(5.0)).isTrue();
            assertThat(rankInfo.isTopPercent(10.0)).isTrue();
        }

        @Test
        @DisplayName("백분위가 임계값 초과이면 false를 반환한다")
        void should_returnFalse_when_exceedsThreshold() {
            RankInfo rankInfo = RankInfo.of(1, 5.1, 2000);

            assertThat(rankInfo.isTopPercent(5.0)).isFalse();
        }
    }
}
