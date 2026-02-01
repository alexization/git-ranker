package com.gitranker.api.domain.badge;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * SVG 배지 렌더링을 담당하는 컴포넌트.
 */
@Component
@RequiredArgsConstructor
public class SvgBadgeRenderer {

    private static final String GITHUB_LOGO_PATH = "M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.137 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z";
    private static final String FONT_IMPORT_CSS = "@import url('https://fonts.googleapis.com/css2?&family=Noto+Sans+KR:wght@400;500;700&family=Outfit:wght@400;500;700;900&display=swap');";

    private final TierGradientProvider gradientProvider;
    private final BadgeFormatter formatter;

    public String render(User user, Tier tier, ActivityLog activityLog) {
        String gradientDefs = gradientProvider.getGradientDefs(tier);
        String displayTierName = formatter.formatTierName(tier.name());
        int tierFontSize = formatter.calculateTierFontSize(displayTierName);

        return String.format(SVG_TEMPLATE,
                gradientDefs,
                FONT_IMPORT_CSS,
                tierFontSize,
                GITHUB_LOGO_PATH,
                user.getUsername(),
                displayTierName,
                formatter.formatNumber(user.getTotalScore()),
                user.getPercentile(),
                formatter.formatNumber(user.getRanking()),
                formatter.formatCount(activityLog.getCommitCount()), formatter.formatDiff(activityLog.getDiffCommitCount()),
                formatter.formatCount(activityLog.getIssueCount()), formatter.formatDiff(activityLog.getDiffIssueCount()),
                formatter.formatCount(activityLog.getPrCount()), formatter.formatDiff(activityLog.getDiffPrCount()),
                formatter.formatCount(activityLog.getMergedPrCount()), formatter.formatDiff(activityLog.getDiffMergedPrCount()),
                formatter.formatCount(activityLog.getReviewCount()), formatter.formatDiff(activityLog.getDiffReviewCount())
        );
    }

    private static final String SVG_TEMPLATE = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <svg width="350" height="170" viewBox="0 0 350 170" fill="none" role="img" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" xml:space="preserve">
                <defs>
                    %s
                    <clipPath id="card-clip">
                        <rect x="0" y="0" width="350" height="170" rx="12" ry="12"/>
                    </clipPath>

                    <linearGradient id="static-gloss" x1="0%%" y1="0%%" x2="0%%" y2="100%%">
                        <stop offset="0%%" style="stop-color:#ffffff;stop-opacity:0.2" />
                        <stop offset="100%%" style="stop-color:#ffffff;stop-opacity:0" />
                    </linearGradient>

                    <linearGradient id="soft-shine-gradient" x1="0%%" y1="0%%" x2="100%%" y2="0%%">
                        <stop offset="0%%" style="stop-color:#ffffff;stop-opacity:0" />
                        <stop offset="50%%" style="stop-color:#ffffff;stop-opacity:0.4" />
                        <stop offset="100%%" style="stop-color:#ffffff;stop-opacity:0" />
                    </linearGradient>
                </defs>

                <style type="text/css">
                    <![CDATA[
                    %s

                    @keyframes soft-pass {
                        0%% { transform: translateX(-400px) skewX(-25deg); }
                        50%% { transform: translateX(-400px) skewX(-35deg); }
                        100%% { transform: translateX(500px) skewX(-35deg); }
                    }

                    .shine-bar {
                        animation: soft-pass 5s infinite ease-in-out;
                        opacity: 0.20;
                    }

                    text {
                        fill: #ffffff;
                        text-rendering: geometricPrecision;
                        -webkit-font-smoothing: antialiased;
                    }

                    .text-shadow { filter: drop-shadow(0px 1px 2px rgba(0, 0, 0, 0.5)); }
                    .text-shadow-strong { filter: drop-shadow(0px 2px 4px rgba(0, 0, 0, 0.7)); }

                    .header {
                        font-family: 'Noto Sans KR', sans-serif;
                        font-weight: 700;
                        font-size: 12px;
                        letter-spacing: 0px;
                    }

                    .username {
                        font-family: 'Noto Sans KR', sans-serif;
                        font-weight: 500;
                        font-size: 13px;
                        opacity: 0.95;
                    }

                    .stat-label {
                        font-family: 'Noto Sans KR', sans-serif;
                        font-size: 11px;
                        opacity: 0.85;
                        letter-spacing: 0.5px;
                        font-weight: 700;
                    }

                    .stat-value {
                        font-family: 'Noto Sans KR', sans-serif;
                        font-weight: 700;
                        font-size: 13px;
                    }

                    .tier-text {
                        font-family: 'Noto Sans KR', sans-serif;
                        font-weight: 500;
                        font-size: %dpx;
                        letter-spacing: 0.5px;
                    }

                    .score-text {
                        font-family: 'Noto Sans KR', sans-serif;
                        font-weight: 700;
                        font-size: 22px;
                        letter-spacing: 0px;
                    }

                    .rank-text {
                        font-family: 'Noto Sans KR', sans-serif;
                        font-size: 11px;
                        font-weight: 500;
                        opacity: 0.9;
                    }

                    .diff-plus { fill: #4ADE80; font-weight: 700; font-size: 11px; font-family: 'Noto Sans KR', sans-serif; }
                    .diff-minus { fill: #FF6B6B; font-weight: 700; font-size: 11px; font-family: 'Noto Sans KR', sans-serif; }
                    ]]>
                </style>

                <rect x="0" y="0" width="350" height="170" rx="12" ry="12" fill="url(#tierGradient)" shape-rendering="geometricPrecision" />

                <g clip-path="url(#card-clip)">
                    <path d="%s" fill="white" fill-opacity="0.08" transform="translate(200, -20) scale(9)" shape-rendering="geometricPrecision"/>

                    <rect x="0" y="0" width="350" height="85" fill="url(#static-gloss)" />
                    <rect class="shine-bar" x="0" y="-30" width="200" height="230" fill="url(#soft-shine-gradient)" />
                </g>

                <text x="20" y="28" class="base-text header text-shadow">Git Ranker</text>
                <text x="330" y="28" text-anchor="end" class="base-text username text-shadow">@%s</text>
                <line x1="20" y1="40" x2="330" y2="40" stroke="#ffffff" stroke-width="1" stroke-opacity="0.4" shape-rendering="crispEdges"/>

                <g transform="translate(20, 85)">
                    <text x="0" y="0" class="base-text tier-text text-shadow-strong">%s</text>
                    <text x="0" y="30" class="mono-text score-text text-shadow">%s pts</text>
                    <text x="0" y="52" class="base-text rank-text text-shadow">Top %.2f%% • Rank %s</text>
                </g>

                <line x1="165" y1="55" x2="165" y2="155" stroke="#ffffff" stroke-width="1" stroke-opacity="0.3" shape-rendering="crispEdges"/>

                <g transform="translate(180, 60)">
                    <g transform="translate(0, 0)">
                        <text x="0" y="0" class="base-text stat-label">Commits</text>
                        <text x="0" y="18" class="mono-text stat-value text-shadow">%s %s</text>
                    </g>
                    <g transform="translate(85, 0)">
                        <text x="0" y="0" class="base-text stat-label">Issues</text>
                        <text x="0" y="18" class="mono-text stat-value text-shadow">%s %s</text>
                    </g>
                    <g transform="translate(0, 34)">
                        <text x="0" y="0" class="base-text stat-label">PR Open</text>
                        <text x="0" y="18" class="mono-text stat-value text-shadow">%s %s</text>
                    </g>
                    <g transform="translate(85, 34)">
                        <text x="0" y="0" class="base-text stat-label">PR Merged</text>
                        <text x="0" y="18" class="mono-text stat-value text-shadow">%s %s</text>
                    </g>
                    <g transform="translate(0, 68)">
                        <text x="0" y="0" class="base-text stat-label">Reviews</text>
                        <text x="0" y="18" class="mono-text stat-value text-shadow">%s %s</text>
                    </g>
                </g>
            </svg>
            """;
}
