package com.gitranker.api.domain.user.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreTest {

    @Nested
    @DisplayName("calculate")
    class Calculate {

        @Test
        @DisplayName("가중치 공식에 따라 정확한 점수를 계산한다")
        void should_calculateCorrectScore_when_allActivitiesProvided() {
            // commits=10*1 + issues=5*2 + reviews=3*5 + prOpened=2*5 + prMerged=1*8 = 10+10+15+10+8 = 53
            Score score = Score.calculate(10, 5, 3, 2, 1);

            assertThat(score.getValue()).isEqualTo(53);
        }

        @Test
        @DisplayName("모든 활동이 0이면 점수도 0이다")
        void should_returnZero_when_allActivitiesAreZero() {
            Score score = Score.calculate(0, 0, 0, 0, 0);

            assertThat(score.getValue()).isZero();
        }

        @Test
        @DisplayName("PR Merged 가중치(8)가 가장 높다")
        void should_weightPrMergedHighest() {
            Score onlyMerged = Score.calculate(0, 0, 0, 0, 1);    // 8
            Score onlyCommit = Score.calculate(1, 0, 0, 0, 0);    // 1
            Score onlyIssue = Score.calculate(0, 1, 0, 0, 0);     // 2
            Score onlyReview = Score.calculate(0, 0, 1, 0, 0);    // 5
            Score onlyPrOpened = Score.calculate(0, 0, 0, 1, 0);  // 5

            assertThat(onlyMerged.getValue()).isGreaterThan(onlyPrOpened.getValue());
            assertThat(onlyMerged.getValue()).isGreaterThan(onlyReview.getValue());
            assertThat(onlyMerged.getValue()).isGreaterThan(onlyIssue.getValue());
            assertThat(onlyMerged.getValue()).isGreaterThan(onlyCommit.getValue());
        }

        @ParameterizedTest
        @DisplayName("각 활동별 가중치가 올바르게 적용된다")
        @CsvSource({
                "1, 0, 0, 0, 0, 1",   // commit * 1
                "0, 1, 0, 0, 0, 2",   // issue * 2
                "0, 0, 1, 0, 0, 5",   // review * 5
                "0, 0, 0, 1, 0, 5",   // prOpened * 5
                "0, 0, 0, 0, 1, 8"    // prMerged * 8
        })
        void should_applyCorrectWeight_for_eachActivity(
                int commits, int issues, int reviews, int prOpened, int prMerged, int expected) {
            Score score = Score.calculate(commits, issues, reviews, prOpened, prMerged);

            assertThat(score.getValue()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("of / zero")
    class FactoryMethods {

        @Test
        @DisplayName("of로 특정 점수의 Score를 생성한다")
        void should_createScore_when_validValue() {
            Score score = Score.of(1500);

            assertThat(score.getValue()).isEqualTo(1500);
        }

        @Test
        @DisplayName("zero는 0점 Score를 반환한다")
        void should_returnZeroScore_when_callingZero() {
            Score score = Score.zero();

            assertThat(score.getValue()).isZero();
        }

        @Test
        @DisplayName("음수 값으로 생성하면 예외가 발생한다")
        void should_throwException_when_negativeValue() {
            assertThatThrownBy(() -> Score.of(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("비교 연산")
    class Comparison {

        @Test
        @DisplayName("높은 점수가 낮은 점수보다 크다고 판별한다")
        void should_returnTrue_when_higherScore() {
            Score higher = Score.of(100);
            Score lower = Score.of(50);

            assertThat(higher.isHigherThan(lower)).isTrue();
            assertThat(lower.isHigherThan(higher)).isFalse();
        }

        @Test
        @DisplayName("같은 점수는 크지 않다고 판별한다")
        void should_returnFalse_when_equalScore() {
            Score a = Score.of(100);
            Score b = Score.of(100);

            assertThat(a.isHigherThan(b)).isFalse();
        }

        @Test
        @DisplayName("두 점수의 차이를 정확히 계산한다")
        void should_calculateDifference() {
            Score a = Score.of(100);
            Score b = Score.of(30);

            assertThat(a.differenceFrom(b)).isEqualTo(70);
            assertThat(b.differenceFrom(a)).isEqualTo(-70);
        }
    }

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("같은 값의 Score는 동등하다")
        void should_beEqual_when_sameValue() {
            Score a = Score.of(500);
            Score b = Score.of(500);

            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("다른 값의 Score는 동등하지 않다")
        void should_notBeEqual_when_differentValue() {
            Score a = Score.of(500);
            Score b = Score.of(501);

            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("동등한 객체는 같은 hashCode를 가진다")
        void should_haveSameHashCode_when_equal() {
            Score a = Score.of(500);
            Score b = Score.of(500);

            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }
    }
}
