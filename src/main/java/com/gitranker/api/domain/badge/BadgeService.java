package com.gitranker.api.domain.badge;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.exception.BusinessException;
import com.gitranker.api.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private static final String GITHUB_LOGO_PATH = "M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.137 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z";
    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;

    @Transactional(readOnly = true)
    public String generateBadge(String nodeId) {
        User user = userRepository.findByNodeId(nodeId).orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));
        ActivityLog activityLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        log.info("사용자 배지 생성 완료: {}", user.getUsername());
        return createSvgContent(user, activityLog);
    }

    private String createSvgContent(User user, ActivityLog activityLog) {
        Tier tier = user.getTier();
        String gradientDefs = getTierGradientDefs(tier);

        int tierFontSize = 28;
        if (tier.name().length() > 9) {
            tierFontSize = 18;
        } else if (tier.name().length() > 6) {
            tierFontSize = 24;
        }

        String animationStyle = """
                    @keyframes soft-pass {
                        0%% { transform: translateX(-400px) skewX(-25deg); }
                        100%% { transform: translateX(500px) skewX(-35deg); }
                    }
                    .shine-bar {
                        animation: soft-pass 5s infinite ease-in-out;
                        opacity: %s;
                    }
                """.formatted("0.45");

        return String.format("""
                <svg width="350" height="170" viewBox="0 0 350 170" xmlns="http://www.w3.org/2000/svg">
                    <defs>
                        %s
                        <clipPath id="card-clip">
                            <rect x="0" y="0" width="350" height="170" rx="12" ry="12"/>
                        </clipPath>
                        <linearGradient id="static-gloss" x1="0%%" y1="0%%" x2="0%%" y2="100%%">
                            <stop offset="0%%" style="stop-color:#ffffff;stop-opacity:0.3" />
                            <stop offset="100%%" style="stop-color:#ffffff;stop-opacity:0" />
                        </linearGradient>
                
                        <linearGradient id="soft-shine-gradient" x1="0%%" y1="0%%" x2="100%%" y2="0%%">
                            <stop offset="0%%" style="stop-color:#ffffff;stop-opacity:0" />
                            <stop offset="50%%" style="stop-color:#ffffff;stop-opacity:0.2" /> <stop offset="100%%" style="stop-color:#ffffff;stop-opacity:0" />
                        </linearGradient>
                    </defs>
                
                    <rect x="0" y="0" width="350" height="170" rx="12" ry="12" fill="url(#tierGradient)" />
                
                    <g clip-path="url(#card-clip)">
                        <g transform="translate(230, 45) scale(4.5)" opacity="0.15" fill="#ffffff">
                            <path d="%s"/>
                        </g>
                
                        <rect x="0" y="0" width="350" height="85" fill="url(#static-gloss)" />
                        <rect class="shine-bar" x="0" y="-30" width="200" height="230" fill="url(#soft-shine-gradient)" />
                    </g>
                
                    <rect x="1" y="1" width="348" height="168" rx="11" ry="11" fill="none" stroke="#ffffff" stroke-opacity="0.5" stroke-width="1.5"/>
                
                <style>
                    /* 폰트 스택 개선: 시스템 폰트를 최우선으로 적용하여 모바일 호환성 확보 */
                    .base-text { font-family: -apple-system, Arial, 'Segoe UI', Roboto, Helvetica, sans-serif; }
                    .mono-text { font-family: 'Monaco', 'JetBrains Mono', 'Fira Code', 'Consolas', 'Andale Mono', 'Ubuntu Mono', monospace; }
                
                    /* SVG Filter 대신 CSS text-shadow 사용 (모바일 렌더링 품질 개선) */
                    .text-shadow { text-shadow: 0px 1px 3px rgba(0, 0, 0, 0.4); }
                    .text-shadow-strong { text-shadow: 0px 2px 4px rgba(0, 0, 0, 0.5); }
                
                    .header { font-weight: 600; font-size: 16px; fill: #ffffff; }
                    .username { font-weight: 400; font-size: 12px; fill: #f0f6fc; opacity: 0.95; }
                
                    /* 작은 텍스트는 그림자를 제거하거나 약하게 주어 가독성 확보 */
                    .stat-label { font-weight: 600; font-size: 9px; fill: #e6edf3; opacity: 0.9; text-transform: uppercase; letter-spacing: 0.5px; text-shadow: 0px 1px 2px rgba(0,0,0,0.3); }
                    .stat-value { font-weight: 600; font-size: 13px; fill: #ffffff; }
                
                    .tier-text {
                        font-weight: 700;
                        font-size: %dpx;
                        fill: #ffffff;
                    }
                
                    .score-text { font-weight: 700; font-size: 20px; fill: #ffffff; opacity: 1; }
                    .rank-text { font-weight: 500; font-size: 11px; fill: #ffffff; opacity: 0.95; }
                
                    .diff-plus { font-weight: 600; font-size: 12px; fill: #4ADE80; text-shadow: none; }
                    .diff-minus { font-weight: 600; font-size: 12px; fill: #F87171; text-shadow: none; }
                
                    %s
                </style>
                
                    <text x="20" y="28" class="base-text header text-shadow">Git Ranker</text>
                    <text x="330" y="28" text-anchor="end" class="base-text username text-shadow">@%s</text>
                    <line x1="20" y1="40" x2="330" y2="40" stroke="#ffffff" stroke-width="1" stroke-opacity="0.4"/>
                
                    <g transform="translate(20, 85)">
                        <text x="0" y="0" class="base-text tier-text text-shadow-strong">%s</text>
                        <text x="0" y="30" class="mono-text score-text text-shadow">%d pts</text>
                        <text x="0" y="52" class="base-text rank-text text-shadow">Top %.2f%% • Rank %d</text>
                    </g>
                
                    <line x1="165" y1="55" x2="165" y2="155" stroke="#ffffff" stroke-width="1" stroke-opacity="0.3"/>
                
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
                """, gradientDefs, GITHUB_LOGO_PATH, tierFontSize, animationStyle, user.getUsername(), user.getTier().name(), user.getTotalScore(), user.getPercentile(), user.getRanking(), formatCount(activityLog.getCommitCount()), formatDiff(activityLog.getDiffCommitCount()), formatCount(activityLog.getIssueCount()), formatDiff(activityLog.getDiffIssueCount()), formatCount(activityLog.getPrCount()), formatDiff(activityLog.getDiffPrCount()), formatCount(activityLog.getMergedPrCount()), formatDiff(activityLog.getDiffMergedPrCount()), formatCount(activityLog.getReviewCount()), formatDiff(activityLog.getDiffReviewCount()));
    }

    private String formatCount(int count) {
        return String.format("%,d", count);
    }

    private String formatDiff(int diff) {
        if (diff > 0) return String.format("<tspan class='diff-plus' dy='-1'>+%d</tspan>", diff);
        if (diff < 0) return String.format("<tspan class='diff-minus' dy='-1'>-%d</tspan>", Math.abs(diff));
        return "";
    }

    private String getTierGradientDefs(Tier tier) {
        String color1, color2, color3;

        switch (tier) {
            case CHALLENGER -> {
                color1 = "#09203F"; // Deep Blue
                color2 = "#1E90FF"; // Bright Blue
                color3 = "#F4D03F"; // Gold
            }
            case MASTER -> {
                color1 = "#3B0B59"; // Deep Purple
                color2 = "#C633F2"; // Vivid Purple
                color3 = "#E066FF"; // Light Purple
            }
            case DIAMOND -> {
                color1 = "#081549"; // Deep Navy
                color2 = "#3375FF"; // Diamond Blue
                color3 = "#63A4FF"; // Light Blue
            }
            case EMERALD -> {
                color1 = "#064E3B"; // Dark Green
                color2 = "#10B981"; // Emerald Green
                color3 = "#4ADE80"; // Light Lime
            }
            case PLATINUM -> {
                color1 = "#0F5963"; // Dark Teal
                color2 = "#00BCD4"; // Cyan
                color3 = "#5FFBF1"; // Bright Mint
            }
            case GOLD -> {
                color1 = "#8E6310"; // Dark Gold
                color2 = "#C2971F"; // Pure Gold
                color3 = "#F4D03F"; // Bright Yellow
            }
            case SILVER -> {
                color1 = "#5F6A6A"; // Dark Silver
                color2 = "#95A5A6"; // Silver
                color3 = "#C0C0C0"; // Light Silver
            }
            case BRONZE -> {
                color1 = "#6E2C00"; // Dark Bronze
                color2 = "#A0522D"; // Bronze
                color3 = "#E6A57E"; // Light Copper
            }
            case IRON -> {
                color1 = "#2B2D42"; // Gunmetal
                color2 = "#4F5D75"; // Steel
                color3 = "#8D99AE"; // Light Steel
            }
            default -> {
                color1 = "#232526";
                color2 = "#414345";
                color3 = "#7B7D7E";
            }
        }

        return String.format("""
                <linearGradient id="tierGradient" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                    <stop offset="0%%" style="stop-color:%s;stop-opacity:1" />
                    <stop offset="50%%" style="stop-color:%s;stop-opacity:1" />
                    <stop offset="100%%" style="stop-color:%s;stop-opacity:1" />
                </linearGradient>
                """, color1, color2, color3);
    }
}