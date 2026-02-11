package com.gitranker.api.domain.user.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityStatisticsTest {

    @Nested
    @DisplayName("calculateScore")
    class CalculateScore {

        @Test
        @DisplayName("통계에서 점수를 정확하게 계산한다")
        void should_calculateCorrectScore() {
            ActivityStatistics stats = ActivityStatistics.of(100, 20, 10, 5, 15);
            Score score = stats.calculateScore();

            // commits=100*1 + issues=20*2 + reviews=15*5 + prOpened=10*5 + prMerged=5*8 = 100+40+75+50+40 = 305
            assertThat(score.getValue()).isEqualTo(305);
        }
    }

    @Nested
    @DisplayName("empty")
    class Empty {

        @Test
        @DisplayName("빈 통계는 모든 값이 0이다")
        void should_returnAllZeros() {
            ActivityStatistics empty = ActivityStatistics.empty();

            assertThat(empty.getCommitCount()).isZero();
            assertThat(empty.getIssueCount()).isZero();
            assertThat(empty.getPrOpenedCount()).isZero();
            assertThat(empty.getPrMergedCount()).isZero();
            assertThat(empty.getReviewCount()).isZero();
        }

        @Test
        @DisplayName("빈 통계는 활동이 없다고 판별한다")
        void should_returnFalse_for_hasActivity() {
            assertThat(ActivityStatistics.empty().hasActivity()).isFalse();
        }
    }

    @Nested
    @DisplayName("calculateDiff")
    class CalculateDiff {

        @Test
        @DisplayName("이전 통계와의 차이를 정확하게 계산한다")
        void should_calculateDiffCorrectly() {
            ActivityStatistics current = ActivityStatistics.of(50, 10, 8, 3, 12);
            ActivityStatistics previous = ActivityStatistics.of(40, 8, 5, 2, 10);

            ActivityStatistics diff = current.calculateDiff(previous);

            assertThat(diff.getCommitCount()).isEqualTo(10);
            assertThat(diff.getIssueCount()).isEqualTo(2);
            assertThat(diff.getPrOpenedCount()).isEqualTo(3);
            assertThat(diff.getPrMergedCount()).isEqualTo(1);
            assertThat(diff.getReviewCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("동일한 통계의 diff는 모두 0이다")
        void should_returnZeroDiff_when_sameStatistics() {
            ActivityStatistics stats = ActivityStatistics.of(10, 5, 3, 2, 4);

            ActivityStatistics diff = stats.calculateDiff(stats);

            assertThat(diff).isEqualTo(ActivityStatistics.empty());
        }
    }

    @Nested
    @DisplayName("merge")
    class Merge {

        @Test
        @DisplayName("두 통계를 합산한다")
        void should_mergeCorrectly() {
            ActivityStatistics a = ActivityStatistics.of(10, 5, 3, 2, 4);
            ActivityStatistics b = ActivityStatistics.of(20, 3, 1, 1, 6);

            ActivityStatistics merged = a.merge(b);

            assertThat(merged.getCommitCount()).isEqualTo(30);
            assertThat(merged.getIssueCount()).isEqualTo(8);
            assertThat(merged.getPrOpenedCount()).isEqualTo(4);
            assertThat(merged.getPrMergedCount()).isEqualTo(3);
            assertThat(merged.getReviewCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("빈 통계와 merge하면 원본과 동일하다")
        void should_returnSame_when_mergedWithEmpty() {
            ActivityStatistics stats = ActivityStatistics.of(10, 5, 3, 2, 4);

            ActivityStatistics merged = stats.merge(ActivityStatistics.empty());

            assertThat(merged).isEqualTo(stats);
        }
    }

    @Nested
    @DisplayName("hasActivity")
    class HasActivity {

        @Test
        @DisplayName("하나라도 활동이 있으면 true를 반환한다")
        void should_returnTrue_when_anyActivityExists() {
            assertThat(ActivityStatistics.of(1, 0, 0, 0, 0).hasActivity()).isTrue();
            assertThat(ActivityStatistics.of(0, 1, 0, 0, 0).hasActivity()).isTrue();
            assertThat(ActivityStatistics.of(0, 0, 1, 0, 0).hasActivity()).isTrue();
            assertThat(ActivityStatistics.of(0, 0, 0, 1, 0).hasActivity()).isTrue();
            assertThat(ActivityStatistics.of(0, 0, 0, 0, 1).hasActivity()).isTrue();
        }
    }

    @Nested
    @DisplayName("totalActivityCount")
    class TotalActivityCount {

        @Test
        @DisplayName("전체 활동 수를 정확하게 합산한다")
        void should_sumAllActivities() {
            ActivityStatistics stats = ActivityStatistics.of(10, 5, 3, 2, 4);

            assertThat(stats.totalActivityCount()).isEqualTo(24);
        }
    }

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("같은 값의 통계는 동등하다")
        void should_beEqual_when_sameValues() {
            ActivityStatistics a = ActivityStatistics.of(10, 5, 3, 2, 4);
            ActivityStatistics b = ActivityStatistics.of(10, 5, 3, 2, 4);

            assertThat(a).isEqualTo(b);
        }
    }
}
