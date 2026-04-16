package com.gitranker.api.domain.badge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BadgeFormatterTest {

    private final BadgeFormatter badgeFormatter = new BadgeFormatter();

    @Test
    @DisplayName("숫자와 카운트를 천 단위 구분으로 포맷한다")
    void formatsNumbersWithGrouping() {
        assertThat(badgeFormatter.formatNumber(1234567L)).isEqualTo("1,234,567");
        assertThat(badgeFormatter.formatCount(12345)).isEqualTo("12,345");
    }

    @Test
    @DisplayName("diff 값은 양수, 음수, 0에 따라 다른 마크업을 만든다")
    void formatsDiffMarkupBySign() {
        assertThat(badgeFormatter.formatDiff(5)).contains("diff-plus").contains("+5");
        assertThat(badgeFormatter.formatDiff(-7)).contains("diff-minus").contains("-7");
        assertThat(badgeFormatter.formatDiff(0)).isEmpty();
    }

    @Test
    @DisplayName("티어 이름은 첫 글자만 대문자로 포맷한다")
    void formatsTierName() {
        assertThat(badgeFormatter.formatTierName("DIAMOND")).isEqualTo("Diamond");
    }

    @Test
    @DisplayName("표시 이름 길이에 따라 티어 폰트 크기를 계산한다")
    void calculatesTierFontSizeByLength() {
        assertThat(badgeFormatter.calculateTierFontSize("Gold")).isEqualTo(32);
        assertThat(badgeFormatter.calculateTierFontSize("Diamond")).isEqualTo(30);
        assertThat(badgeFormatter.calculateTierFontSize("Challenger")).isEqualTo(26);
    }

    @Test
    @DisplayName("폰트 크기 경계값 6자와 7자는 서로 다른 크기를 사용한다")
    void calculatesTierFontSizeAtBoundaries() {
        assertThat(badgeFormatter.calculateTierFontSize("Silver")).isEqualTo(32);
        assertThat(badgeFormatter.calculateTierFontSize("MasterX")).isEqualTo(30);
    }
}
