package com.gitranker.api.domain.user.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class Score {

    private static final int COMMIT_WEIGHT = 1;
    private static final int ISSUE_WEIGHT = 2;
    private static final int REVIEW_WEIGHT = 5;
    private static final int PR_OPENED_WEIGHT = 5;
    private static final int PR_MERGED_WEIGHT = 8;

    @Column(name = "total_score", nullable = false)
    private int value;

    private Score(int value) {
        validateNonNegative(value);
        this.value = value;
    }

    public static Score calculate(int commits, int issues, int reviews, int prOpened, int prMerged) {
        int totalScore = (commits * COMMIT_WEIGHT)
                         + (issues * ISSUE_WEIGHT)
                         + (reviews * REVIEW_WEIGHT)
                         + (prOpened * PR_OPENED_WEIGHT)
                         + (prMerged * PR_MERGED_WEIGHT);

        return new Score(totalScore);
    }

    public static Score of(int value) {
        return new Score(value);
    }

    public static Score zero() {
        return new Score(0);
    }

    public boolean isHigherThan(Score other) {
        return this.value > other.value;
    }

    public int differenceFrom(Score other) {
        return this.value - other.value;
    }

    private void validateNonNegative(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("점수는 음수가 될 수 없습니다: " + value);
        }
    }

    @Override
    public String toString() {
        return String.format("%,d pts", value);
    }
}
