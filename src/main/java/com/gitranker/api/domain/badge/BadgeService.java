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

    @Transactional(readOnly = true)
    public String generateBadge(String nodeId) {
        User user = userRepository.findByNodeId(nodeId).orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        ActivityLog activityLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        return createSvgContent(user, activityLog);
    }

    private String createSvgContent(User user, ActivityLog activityLog) {
        Tier tier = user.getTier();
        String gradientDefs = getTierGradientDefs(tier);
        String tierShape = getTierShape(tier);

        return String.format("""
                        <svg width="450" height="200" viewBox="0 0 450 200" xmlns="http://www.w3.org/2000/svg">
                            <defs>
                                %s
                                <filter id="shadow" x="-20%%" y="-20%%" width="140%%" height="140%%">
                                    <feDropShadow dx="1" dy="1" stdDeviation="1" flood-opacity="0.4"/>
                                </filter>
                            </defs>
                        
                            <rect x="0" y="0" width="450" height="200" rx="15" ry="15" fill="url(#tierGradient)" stroke="#ffffff" stroke-opacity="0.1" stroke-width="1"/>
                        
                            <g transform="translate(280, 10) scale(1.2)" opacity="0.1" fill="#ffffff">
                                %s
                            </g>
                        
                            <style>
                                .header { font: 600 18px 'Google Sans', 'Segoe UI', Ubuntu, sans-serif; fill: #ffffff; filter: url(#shadow); }
                                .username { font: 400 14px 'Google Sans', 'Segoe UI', Ubuntu, sans-serif; fill: #f0f6fc; opacity: 0.9; }
                                .stat-label { font: 600 10px 'Google Sans', 'Segoe UI', sans-serif; fill: #e6edf3; opacity: 0.85; text-transform: uppercase; letter-spacing: 0.8px; }
                                .stat-value { font: 600 15px 'Google Sans Mono', 'SF Mono', 'Segoe UI Mono', monospace; fill: #ffffff; filter: url(#shadow); }
                                .tier-text { font: 800 34px 'Google Sans', 'Segoe UI', sans-serif; fill: #ffffff; filter: url(#shadow); letter-spacing: 1px; }
                                .score-text { font: 700 24px 'Google Sans Mono', monospace; fill: #ffffff; opacity: 0.95; }
                                .rank-text { font: 500 13px 'Google Sans', 'Segoe UI', sans-serif; fill: #ffffff; opacity: 0.9; }
                        
                                .diff-plus { font: 600 11px 'Google Sans Mono', monospace; fill: #3fb950; }
                                .diff-minus { font: 600 11px 'Google Sans Mono', monospace; fill: #ff7b72; }
                            </style>
                        
                            <text x="30" y="35" class="header">Git Ranker</text>
                            <text x="420" y="35" text-anchor="end" class="username">@%s</text>
                        
                            <line x1="30" y1="50" x2="420" y2="50" stroke="#ffffff" stroke-width="1" stroke-opacity="0.3"/>
                        
                            <g transform="translate(30, 95)">
                                <text x="0" y="0" class="tier-text">%s</text>
                                <text x="0" y="35" class="score-text">%d pts</text>
                                <text x="0" y="60" class="rank-text">Top %.2f%% • Rank %d</text>
                            </g>
                        
                            <line x1="215" y1="70" x2="215" y2="170" stroke="#ffffff" stroke-width="1" stroke-opacity="0.2"/>
                        
                            <g transform="translate(235, 75)">
                                <g transform="translate(0, 0)">
                                    <text x="0" y="0" class="stat-label">Commits</text>
                                    <text x="0" y="20" class="stat-value">%s %s</text>
                                </g>
                                <g transform="translate(100, 0)">
                                    <text x="0" y="0" class="stat-label">Issues</text>
                                    <text x="0" y="20" class="stat-value">%s %s</text>
                                </g>
                        
                                <g transform="translate(0, 38)">
                                    <text x="0" y="0" class="stat-label">PR Open</text>
                                    <text x="0" y="20" class="stat-value">%s %s</text>
                                </g>
                                <g transform="translate(100, 38)">
                                    <text x="0" y="0" class="stat-label">PR Merged</text>
                                    <text x="0" y="20" class="stat-value">%s %s</text>
                                </g>
                        
                                <g transform="translate(0, 76)">
                                    <text x="0" y="0" class="stat-label">Reviews</text>
                                    <text x="0" y="20" class="stat-value">%s %s</text>
                                </g>
                            </g>
                        </svg>
                        """, gradientDefs, tierShape, user.getUsername(), user.getTier().name(), user.getTotalScore(), user.getPercentile(), user.getRanking(),

                formatCount(activityLog.getCommitCount()), formatDiff(activityLog.getDiffCommitCount()), formatCount(activityLog.getIssueCount()), formatDiff(activityLog.getDiffIssueCount()), formatCount(activityLog.getPrCount()), formatDiff(activityLog.getDiffPrCount()), formatCount(activityLog.getMergedPrCount()), formatDiff(activityLog.getDiffMergedPrCount()), formatCount(activityLog.getReviewCount()), formatDiff(activityLog.getDiffReviewCount()));
    }

    private String formatCount(int count) {
        return String.format("%,d", count);
    }

    private String formatDiff(int diff) {
        if (diff > 0) return String.format("<tspan class='diff-plus' dy='-2' font-size='10'>▲%d</tspan>", diff);
        if (diff < 0)
            return String.format("<tspan class='diff-minus' dy='-2' font-size='10'>▼%d</tspan>", Math.abs(diff));
        return "";
    }

    private String getTierGradientDefs(Tier tier) {
        String startColor;
        String endColor;

        switch (tier) {
            case DIAMOND -> { // Cyan Blue
                startColor = "#00B4DB";
                endColor = "#0083B0";
            }
            case PLATINUM -> { // Mint/Teal
                startColor = "#00C9A7";
                endColor = "#008E74";
            }
            case GOLD -> { // Deep Gold
                startColor = "#F1C40F";
                endColor = "#B7880B";
            }
            case SILVER -> { // Metallic Gray
                startColor = "#9CA3AF";
                endColor = "#4B5563";
            }
            case BRONZE -> { // Earthy Copper
                startColor = "#D38D5F";
                endColor = "#8B4513";
            }
            default -> { // IRON
                startColor = "#34495E";
                endColor = "#2C3E50";
            }
        }

        return String.format("""
                <linearGradient id="tierGradient" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                    <stop offset="0%%" style="stop-color:%s;stop-opacity:1" />
                    <stop offset="100%%" style="stop-color:%s;stop-opacity:1" />
                </linearGradient>
                """, startColor, endColor);
    }

    private String getTierShape(Tier tier) {
        return switch (tier) {
            case DIAMOND -> "<path d='M50 0 L100 50 L50 100 L0 50 Z' />";
            case PLATINUM -> "<path d='M50 100 L10 60 Q0 50 10 40 L50 0 L90 40 Q100 50 90 60 Z' />";
            case GOLD -> "<polygon points='50,0 61,35 98,35 68,57 79,91 50,70 21,91 32,57 2,35 39,35' />";
            case SILVER -> "<path d='M50 0 L90 15 V45 C90 75 50 100 50 100 C50 100 10 75 10 45 V15 Z' />";
            case BRONZE -> "<circle cx='50' cy='50' r='40' />";
            default -> "<rect x='10' y='10' width='80' height='80' rx='10' />";
        };
    }
}