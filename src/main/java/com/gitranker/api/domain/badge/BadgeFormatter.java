package com.gitranker.api.domain.badge;

import org.springframework.stereotype.Component;

/**
 * 배지에 표시되는 숫자 및 텍스트 포맷팅을 담당하는 컴포넌트.
 */
@Component
public class BadgeFormatter {

    public String formatNumber(long number) {
        return String.format("%,d", number);
    }

    public String formatCount(int count) {
        return String.format("%,d", count);
    }

    public String formatDiff(int diff) {
        if (diff > 0) {
            return String.format("<tspan class='diff-plus' dy='-1'>+%d</tspan>", diff);
        }
        if (diff < 0) {
            return String.format("<tspan class='diff-minus' dy='-1'>-%d</tspan>", Math.abs(diff));
        }
        return "";
    }

    public String formatTierName(String tierName) {
        return tierName.charAt(0) + tierName.substring(1).toLowerCase();
    }

    public int calculateTierFontSize(String displayTierName) {
        if (displayTierName.length() > 9) {
            return 26;
        } else if (displayTierName.length() > 6) {
            return 30;
        }
        return 32;
    }
}
