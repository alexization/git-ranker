package com.gitranker.api.domain.badge;

import com.gitranker.api.domain.user.Tier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 티어별 그라데이션 색상을 제공하는 컴포넌트.
 */
@Component
public class TierGradientProvider {

    private static final Map<Tier, TierColors> TIER_COLORS = Map.of(
            Tier.CHALLENGER, new TierColors("#09203F", "#3B82F6", "#D4AF37"),
            Tier.MASTER, new TierColors("#2E1065", "#7C3AED", "#F472B6"),
            Tier.DIAMOND, new TierColors("#0C4A6E", "#0284C7", "#7DD3FC"),
            Tier.EMERALD, new TierColors("#064E3B", "#059669", "#34D399"),
            Tier.PLATINUM, new TierColors("#1E293B", "#0F766E", "#2DD4BF"),
            Tier.GOLD, new TierColors("#8E6310", "#C2971F", "#F4D03F"),
            Tier.SILVER, new TierColors("#111827", "#4B5563", "#9CA3AF"),
            Tier.BRONZE, new TierColors("#431407", "#92400E", "#D97706"),
            Tier.IRON, new TierColors("#0F172A", "#334155", "#64748B")
    );

    private static final TierColors DEFAULT_COLORS = new TierColors("#0F172A", "#334155", "#64748B");

    public TierColors getColors(Tier tier) {
        return TIER_COLORS.getOrDefault(tier, DEFAULT_COLORS);
    }

    public String getGradientDefs(Tier tier) {
        TierColors colors = getColors(tier);
        return String.format("""
                <linearGradient id="tierGradient" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                    <stop offset="0%%" style="stop-color:%s;stop-opacity:1" />
                    <stop offset="50%%" style="stop-color:%s;stop-opacity:1" />
                    <stop offset="100%%" style="stop-color:%s;stop-opacity:1" />
                </linearGradient>
                """, colors.color1(), colors.color2(), colors.color3());
    }

    public record TierColors(String color1, String color2, String color3) {}
}
