package com.gitranker.api.domain.badge;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.exception.BusinessException;
import com.gitranker.api.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;

    private static final String GITHUB_LOGO_PATH = "M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.137 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z";

    @Transactional(readOnly = true)
    public String generateBadge(String nodeId) {
        User user = userRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        ActivityLog activityLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        return createSvgContent(user, activityLog);
    }

    private String createSvgContent(User user, ActivityLog activityLog) {
        Tier tier = user.getTier();
        String gradientDefs = getTierGradientDefs(tier);

        return String.format("""
                        <svg width="350" height="170" viewBox="0 0 350 170" xmlns="http://www.w3.org/2000/svg">
                            <defs>
                                %s
                                <filter id="shadow" x="-20%%" y="-20%%" width="140%%" height="140%%">
                                    <feDropShadow dx="1" dy="1" stdDeviation="1" flood-opacity="0.4"/>
                                </filter>
                            </defs>
                        
                            <rect x="0" y="0" width="350" height="170" rx="12" ry="12" fill="url(#tierGradient)" stroke="#ffffff" stroke-opacity="0.1" stroke-width="1"/>
                        
                            <g transform="translate(230, 45) scale(4.5)" opacity="0.1" fill="#ffffff">
                                <path d="%s"/>
                            </g>
                        
                            <style>
                                .header { font: 600 16px 'Google Sans', 'Segoe UI', Ubuntu, sans-serif; fill: #ffffff; filter: url(#shadow); }
                                .username { font: 400 12px 'Google Sans', 'Segoe UI', Ubuntu, sans-serif; fill: #f0f6fc; opacity: 0.9; }
                                .stat-label { font: 600 9px 'Google Sans', 'Segoe UI', sans-serif; fill: #e6edf3; opacity: 0.85; text-transform: uppercase; letter-spacing: 0.5px; }
                                .stat-value { font: 600 13px 'Google Sans Mono', 'SF Mono', 'Segoe UI Mono', monospace; fill: #ffffff; filter: url(#shadow); }
                                .tier-text { font: 800 28px 'Google Sans', 'Segoe UI', sans-serif; fill: #ffffff; filter: url(#shadow); letter-spacing: 0.5px; }
                                .score-text { font: 700 20px 'Google Sans Mono', monospace; fill: #ffffff; opacity: 0.95; }
                                .rank-text { font: 500 11px 'Google Sans', 'Segoe UI', sans-serif; fill: #ffffff; opacity: 0.9; }
                                
                                .diff-plus { font: 600 10px 'Google Sans Mono', monospace; fill: #3fb950; }
                                .diff-minus { font: 600 10px 'Google Sans Mono', monospace; fill: #ff7b72; }
                            </style>
                        
                            <text x="20" y="28" class="header">Git Ranker</text>
                            <text x="330" y="28" text-anchor="end" class="username">@%s</text>
                            
                            <line x1="20" y1="40" x2="330" y2="40" stroke="#ffffff" stroke-width="1" stroke-opacity="0.3"/>
                        
                            <g transform="translate(20, 85)">
                                <text x="0" y="0" class="tier-text">%s</text>
                                <text x="0" y="30" class="score-text">%d pts</text>
                                <text x="0" y="52" class="rank-text">Top %.2f%% • Rank %d</text>
                            </g>
                            
                            <line x1="165" y1="55" x2="165" y2="155" stroke="#ffffff" stroke-width="1" stroke-opacity="0.2"/>
                            
                            <g transform="translate(180, 60)">
                                <g transform="translate(0, 0)">
                                    <text x="0" y="0" class="stat-label">Commits</text>
                                    <text x="0" y="18" class="stat-value">%s %s</text>
                                </g>
                                <g transform="translate(85, 0)">
                                    <text x="0" y="0" class="stat-label">Issues</text>
                                    <text x="0" y="18" class="stat-value">%s %s</text>
                                </g>
                                
                                <g transform="translate(0, 34)">
                                    <text x="0" y="0" class="stat-label">PR Open</text>
                                    <text x="0" y="18" class="stat-value">%s %s</text>
                                </g>
                                <g transform="translate(85, 34)">
                                    <text x="0" y="0" class="stat-label">PR Merged</text>
                                    <text x="0" y="18" class="stat-value">%s %s</text>
                                </g>
                                
                                <g transform="translate(0, 68)">
                                    <text x="0" y="0" class="stat-label">Reviews</text>
                                    <text x="0" y="18" class="stat-value">%s %s</text>
                                </g>
                            </g>
                        </svg>
                        """,
                gradientDefs,
                GITHUB_LOGO_PATH,
                user.getUsername(),
                user.getTier().name(),
                user.getTotalScore(),
                user.getPercentile(), user.getRanking(),

                formatCount(activityLog.getCommitCount()), formatDiff(activityLog.getDiffCommitCount()),
                formatCount(activityLog.getIssueCount()), formatDiff(activityLog.getDiffIssueCount()),
                formatCount(activityLog.getPrCount()), formatDiff(activityLog.getDiffPrCount()),
                formatCount(activityLog.getMergedPrCount()), formatDiff(activityLog.getDiffMergedPrCount()),
                formatCount(activityLog.getReviewCount()), formatDiff(activityLog.getDiffReviewCount())
        );
    }

    private String formatCount(int count) {
        return String.format("%,d", count);
    }

    private String formatDiff(int diff) {
        if (diff > 0) return String.format("<tspan class='diff-plus' dy='-2' font-size='9'>▲%d</tspan>", diff);
        if (diff < 0) return String.format("<tspan class='diff-minus' dy='-2' font-size='9'>▼%d</tspan>", Math.abs(diff));
        return "";
    }

    private String getTierGradientDefs(Tier tier) {
        String startColor;
        String endColor;

        switch (tier) {
            case DIAMOND -> {
                // [LoL Diamond] Deep Royal Blue (Kept as is)
                startColor = "#005C97";
                endColor = "#363795";
            }
            case PLATINUM -> {
                // [Toned Down] Calm Turquoise -> Deep Teal (Removed Neon Glare)
                startColor = "#45C4B0"; // Calm Turquoise
                endColor = "#014F44";   // Deep Greenish Teal
            }
            case GOLD -> {
                // [Toned Down] Rich Amber -> Dark Bronze (Removed Blinding Yellow)
                startColor = "#F0B429"; // Rich Golden Yellow
                endColor = "#784800";   // Dark Bronze/Brown
            }
            case SILVER -> {
                // [Toned Down] Silver Grey -> Dark Slate (Reduced Whiteness)
                startColor = "#BDC3C7"; // Silver Grey
                endColor = "#2C3E50";   // Dark Slate
            }
            case BRONZE -> {
                // [LoL Bronze] Reddish Copper (Kept as is)
                startColor = "#E86A34";
                endColor = "#411E11";
            }
            default -> {
                // IRON
                startColor = "#232526";
                endColor = "#414345";
            }
        }

        return String.format("""
                <linearGradient id="tierGradient" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                    <stop offset="0%%" style="stop-color:%s;stop-opacity:1" />
                    <stop offset="100%%" style="stop-color:%s;stop-opacity:1" />
                </linearGradient>
                """, startColor, endColor);
    }
}