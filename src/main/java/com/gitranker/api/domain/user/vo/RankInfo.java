package com.gitranker.api.domain.user.vo;

import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.global.error.message.DomainMessages;
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
    private static final double DIAMOND_THRESHOLD = 12.0;
    private static final double EMERALD_THRESHOLD = 25.0;
    private static final double PLATINUM_THRESHOLD = 45.0;

    private static final int MIN_HIGH_TIER_SCORE = 2000;
    private static final int GOLD_SCORE = 1500;
    private static final int SILVER_SCORE = 1000;
    private static final int BRONZE_SCORE = 500;

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

    public static RankInfo of(int ranking, double percentile, int totalScore) {
        Tier tier = calculateTier(percentile, totalScore);

        return new RankInfo(ranking, percentile, tier);
    }

    public static RankInfo calculate(long higherScoreCount, long totalUserCount, int totalScore) {
        if (totalUserCount == 0) {
            return initial();
        }

        int ranking = (int) higherScoreCount + 1;
        double percentile = (double) ranking / totalUserCount * 100.0;

        return of(ranking, percentile, totalScore);
    }

    public static RankInfo initial() {
        return new RankInfo(0, 100.0, Tier.IRON);
    }

    private static Tier calculateTier(double percentile, int totalScore) {
        if (totalScore >= MIN_HIGH_TIER_SCORE) {
            if (percentile <= CHALLENGER_THRESHOLD) return Tier.CHALLENGER;
            if (percentile <= MASTER_THRESHOLD) return Tier.MASTER;
            if (percentile <= DIAMOND_THRESHOLD) return Tier.DIAMOND;
            if (percentile <= EMERALD_THRESHOLD) return Tier.EMERALD;
            if (percentile <= PLATINUM_THRESHOLD) return Tier.PLATINUM;
        }

        if (totalScore >= GOLD_SCORE) return Tier.GOLD;
        if (totalScore >= SILVER_SCORE) return Tier.SILVER;
        if (totalScore >= BRONZE_SCORE) return Tier.BRONZE;

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
            throw new IllegalArgumentException(String.format(DomainMessages.RANKING_CANNOT_BE_NEGATIVE, ranking));
        }
    }

    private void validatePercentile(double percentile) {
        if (percentile < 0 || percentile > 100) {
            throw new IllegalArgumentException(String.format(DomainMessages.PERCENTILE_OUT_OF_RANGE, percentile));
        }
    }

    @Override
    public String toString() {
        return String.format("Rank %,d (Top %.2f) - %s", ranking, percentile, tier);
    }
}
