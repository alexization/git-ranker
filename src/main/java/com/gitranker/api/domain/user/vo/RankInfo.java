package com.gitranker.api.domain.user.vo;

import com.gitranker.api.domain.user.Tier;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class RankInfo {

    private static final double CHALLENGER_THRESHOLD = 1.0;
    private static final double MASTER_THRESHOLD = 5.0;
    private static final double DIAMOND_THRESHOLD = 10.0;
    private static final double EMERALD_THRESHOLD = 25.0;
    private static final double PLATINUM_THRESHOLD = 40.0;
    private static final double GOLD_THRESHOLD = 55.0;
    private static final double SILVER_THRESHOLD = 70.0;
    private static final double BRONZE_THRESHOLD = 90.0;

    @Column(name = "ranking", nullable = false)
    private int ranking;

    @Column(name = "percentile", nullable = false)
    private double percentile;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false)
    private Tier tier;

    private RankInfo(int ranking, double percentile, Tier tier) {
        validateRanking(ranking);
        validatePercentile(percentile);
        this.ranking = ranking;
        this.percentile = percentile;
        this.tier = tier;
    }

    public static RankInfo of(int ranking, double percentile) {
        Tier tier = calculateTier(percentile);

        return new RankInfo(ranking, percentile, tier);
    }

    public static RankInfo calculate(long higherScoreCount, long totalUserCount) {
        if (totalUserCount == 0) {
            return initial();
        }

        int ranking = (int) higherScoreCount + 1;
        double percentile = (double) ranking / totalUserCount * 100.0;

        return of(ranking, percentile);
    }

    public static RankInfo initial() {
        return new RankInfo(0, 100.0, Tier.IRON);
    }

    private static Tier calculateTier(double percentile) {
        if (percentile <= CHALLENGER_THRESHOLD) return Tier.CHALLENGER;
        if (percentile <= MASTER_THRESHOLD) return Tier.MASTER;
        if (percentile <= DIAMOND_THRESHOLD) return Tier.DIAMOND;
        if (percentile <= EMERALD_THRESHOLD) return Tier.EMERALD;
        if (percentile <= PLATINUM_THRESHOLD) return Tier.PLATINUM;
        if (percentile <= GOLD_THRESHOLD) return Tier.GOLD;
        if (percentile <= SILVER_THRESHOLD) return Tier.SILVER;
        if (percentile <= BRONZE_THRESHOLD) return Tier.BRONZE;

        return Tier.IRON;
    }

    public boolean isTierPromoted(RankInfo previous) {
        return this.tier.ordinal() < previous.tier.ordinal();
    }

    public boolean isTopPercent(double threshold) {
        return this.percentile <= threshold;
    }

    private void validateRanking(int ranking) {
        if (ranking < 0) {
            throw new IllegalArgumentException("순위는 음수가 될 수 없습니다.: " + ranking);
        }
    }

    private void validatePercentile(double percentile) {
        if (percentile < 0 || percentile > 100) {
            throw new IllegalArgumentException("백분율은 0~100 사이여야 합니다. " + percentile);
        }
    }

    @Override
    public String toString() {
        return String.format("Rank %,d (Top %.2f) - %s", ranking, percentile, tier);
    }
}
