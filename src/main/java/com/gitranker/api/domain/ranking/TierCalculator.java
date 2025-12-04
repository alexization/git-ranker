package com.gitranker.api.domain.ranking;

import com.gitranker.api.domain.user.Tier;
import org.springframework.stereotype.Component;

@Component
public class TierCalculator {
    private static final double DIAMOND_THRESHOLD = 1.0;
    private static final double PLATINUM_THRESHOLD = 5.0;
    private static final double GOLD_THRESHOLD = 10.0;
    private static final double SILVER_THRESHOLD = 25.0;
    private static final double BRONZE_THRESHOLD = 50.0;

    public Tier calculateTier(double percentile) {
        if (percentile <= DIAMOND_THRESHOLD) {
            return Tier.DIAMOND;
        } else if (percentile <= PLATINUM_THRESHOLD) {
            return Tier.PLATINUM;
        } else if (percentile <= GOLD_THRESHOLD) {
            return Tier.GOLD;
        } else if (percentile <= SILVER_THRESHOLD) {
            return Tier.SILVER;
        } else if (percentile <= BRONZE_THRESHOLD) {
            return Tier.BRONZE;
        } else {
            return Tier.IRON;
        }
    }
}
