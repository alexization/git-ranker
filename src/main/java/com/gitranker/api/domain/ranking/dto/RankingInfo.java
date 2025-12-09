package com.gitranker.api.domain.ranking.dto;

import com.gitranker.api.domain.user.Tier;

public record RankingInfo(
        int ranking,
        double percentile,
        Tier tier
) {
    @Override
    public String toString() {
        return String.format("순위 : %d등, 상위 %.2f%%, 티어 : %s", ranking, percentile, tier);
    }
}
