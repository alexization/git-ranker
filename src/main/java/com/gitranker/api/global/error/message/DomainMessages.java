package com.gitranker.api.global.error.message;

public final class DomainMessages {

    private DomainMessages() {}

    public static final String SCORE_CANNOT_BE_NEGATIVE = "점수는 음수가 될 수 없습니다: %d";

    public static final String RANKING_CANNOT_BE_NEGATIVE = "순위는 음수가 될 수 없습니다: %d";
    public static final String PERCENTILE_OUT_OF_RANGE = "백분율은 0~100 사이여야 합니다: %.2f";
}
