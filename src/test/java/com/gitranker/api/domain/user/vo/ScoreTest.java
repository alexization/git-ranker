package com.gitranker.api.domain.user.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreTest {

    @Test
    @DisplayName("점수는 활동별 가중치를 반영해 계산하고 포맷한다")
    void calculatesWeightedScoreAndFormatsIt() {
        Score score = Score.calculate(10, 2, 3, 4, 1);

        assertThat(score.getValue()).isEqualTo(57);
        assertThat(score.toString()).isEqualTo("57 pts");
    }

    @Test
    @DisplayName("zero와 비교 helper는 점수 비교와 차이를 반환한다")
    void exposesComparisonHelpers() {
        Score current = Score.of(1500);
        Score previous = Score.zero();

        assertThat(current.isHigherThan(previous)).isTrue();
        assertThat(previous.isHigherThan(current)).isFalse();
        assertThat(current.differenceFrom(previous)).isEqualTo(1500);
    }

    @Test
    @DisplayName("음수 점수는 허용하지 않는다")
    void rejectsNegativeScore() {
        assertThatThrownBy(() -> Score.of(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1");
    }
}
