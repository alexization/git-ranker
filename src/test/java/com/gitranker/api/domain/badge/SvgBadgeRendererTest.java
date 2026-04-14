package com.gitranker.api.domain.badge;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.RankInfo;
import com.gitranker.api.domain.user.vo.Score;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.gitranker.api.support.TestFixtures.activityLog;
import static com.gitranker.api.support.TestFixtures.stats;
import static com.gitranker.api.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

class SvgBadgeRendererTest {

    private final SvgBadgeRenderer svgBadgeRenderer =
            new SvgBadgeRenderer(new TierGradientProvider(), new BadgeFormatter());

    @Test
    @DisplayName("SVG 렌더링 결과에는 사용자, 티어, 점수, 활동 통계가 포함된다")
    void rendersSvgWithUserTierAndStats() {
        User user = user("alice");
        user.updateScore(Score.of(4321));
        user.updateRankInfo(RankInfo.of(3, 12.34, 4321));
        ActivityLog activityLog = activityLog(user, stats(120, 8, 15, 10, 23), stats(5, -2, 1, 0, 3));

        String svg = svgBadgeRenderer.render(user, Tier.DIAMOND, activityLog);

        assertThat(svg)
                .contains("@alice")
                .contains("Diamond")
                .contains("4,321 pts")
                .contains("Top 12.34%")
                .contains("Rank 3")
                .contains("120")
                .contains("15")
                .contains("23")
                .contains("diff-plus")
                .contains("+5")
                .contains("diff-minus")
                .contains("-2");
    }
}
