package com.gitranker.api.domain.badge;

import com.gitranker.api.domain.user.Tier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TierGradientProviderTest {

    private final TierGradientProvider tierGradientProvider = new TierGradientProvider();

    @Test
    @DisplayName("알려진 티어는 고정된 색상 세트를 반환한다")
    void returnsColorsForKnownTier() {
        TierGradientProvider.TierColors colors = tierGradientProvider.getColors(Tier.GOLD);

        assertThat(colors.color1()).isEqualTo("#8E6310");
        assertThat(colors.color2()).isEqualTo("#C2971F");
        assertThat(colors.color3()).isEqualTo("#F4D03F");
    }

    @Test
    @DisplayName("IRON 티어는 기본 배지 색상 세트를 사용한다")
    void returnsExpectedColorsForIronTier() {
        TierGradientProvider.TierColors colors = tierGradientProvider.getColors(Tier.IRON);

        assertThat(colors.color1()).isEqualTo("#0F172A");
        assertThat(colors.color2()).isEqualTo("#334155");
        assertThat(colors.color3()).isEqualTo("#64748B");
    }

    @Test
    @DisplayName("그라데이션 정의에는 선택된 티어의 세 색상이 모두 포함된다")
    void gradientDefinitionContainsTierColors() {
        String gradientDefs = tierGradientProvider.getGradientDefs(Tier.EMERALD);

        assertThat(gradientDefs)
                .contains("#064E3B")
                .contains("#059669")
                .contains("#34D399");
    }
}
