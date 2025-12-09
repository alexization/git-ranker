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
        User user = userRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        ActivityLog activityLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        return createSvgContent(user, activityLog);
    }

    private String createSvgContent(User user, ActivityLog activityLog) {
        String backgroundColor = getTierBackgroundColor(user.getTier());
        String tierColor = getTierTextColor(user.getTier());

        return String.format("""
                        <svg width="450" height="200" viewBox="0 0 450 200" xmlns="http://www.w3.org/2000/svg">
                            <style>
                                .header { font: 600 18px 'Segoe UI', Ubuntu, Sans-Serif; fill: #ffffff; }
                                .stat-label { font: 400 12px 'Segoe UI', Ubuntu, Sans-Serif; fill: #c9d1d9; }
                                .stat-value { font: 600 14px 'Segoe UI', Ubuntu, Sans-Serif; fill: #ffffff; }
                                .tier-text { font: 700 24px 'Segoe UI', Ubuntu, Sans-Serif; fill: %s; }
                                .score-text { font: 600 30px 'Segoe UI', Ubuntu, Sans-Serif; fill: #ffffff; }
                                .rank-text { font: 400 12px 'Segoe UI', Ubuntu, Sans-Serif; fill: #8b949e; }
                                .diff-plus { font: 400 10px 'Segoe UI', Ubuntu, Sans-Serif; fill: #3fb950; }
                                .diff-minus { font: 400 10px 'Segoe UI', Ubuntu, Sans-Serif; fill: #f85149; }
                                .diff-zero { font: 400 10px 'Segoe UI', Ubuntu, Sans-Serif; fill: #8b949e; }
                            </style>
                        
                            <rect x="0" y="0" width="450" height="200" rx="10" ry="10" fill="%s" stroke="#30363d" stroke-width="1"/>
                        
                            <text x="25" y="35" class="header">Git Ranker</text>
                            <text x="425" y="35" text-anchor="end" class="header" font-weight="400">@%s</text>
                            <line x1="25" y1="50" x2="425" y2="50" stroke="#30363d" stroke-width="1"/>
                        
                            <g transform="translate(25, 80)">
                                <text x="0" y="25" class="tier-text">%s</text>
                                <text x="0" y="60" class="score-text">%d pts</text>
                                <text x="0" y="85" class="rank-text">Top %.2f%% (Rank %d)</text>
                            </g>
                        
                            <line x1="180" y1="65" x2="180" y2="175" stroke="#30363d" stroke-width="1"/>
                        
                            <g transform="translate(200, 70)">
                                <g transform="translate(0, 0)">
                                    <text x="0" y="10" class="stat-label">Commits</text>
                                    <text x="0" y="30" class="stat-value">%d %s</text>
                                </g>
                                <g transform="translate(120, 0)">
                                    <text x="0" y="10" class="stat-label">Issues</text>
                                    <text x="0" y="30" class="stat-value">%d %s</text>
                                </g>
                        
                                <g transform="translate(0, 45)">
                                    <text x="0" y="10" class="stat-label">PR Open</text>
                                    <text x="0" y="30" class="stat-value">%d %s</text>
                                </g>
                                <g transform="translate(120, 45)">
                                    <text x="0" y="10" class="stat-label">PR Merged</text>
                                    <text x="0" y="30" class="stat-value">%d %s</text>
                                </g>
                        
                                <g transform="translate(0, 90)">
                                    <text x="0" y="10" class="stat-label">Reviews</text>
                                    <text x="0" y="30" class="stat-value">%d %s</text>
                                </g>
                            </g>
                        </svg>
                        """,
                tierColor, // style .tier-text color
                backgroundColor, // rect fill
                user.getUsername(),
                user.getTier().name(),
                user.getTotalScore(),
                user.getPercentile(), user.getRanking(),

                activityLog.getCommitCount(), formatDiff(activityLog.getDiffCommitCount()),
                activityLog.getIssueCount(), formatDiff(activityLog.getDiffIssueCount()),
                activityLog.getPrCount(), formatDiff(activityLog.getDiffPrCount()),
                activityLog.getMergedPrCount(), formatDiff(activityLog.getDiffMergedPrCount()),
                activityLog.getReviewCount(), formatDiff(activityLog.getDiffReviewCount())
        );
    }

    private String formatDiff(int diff) {
        if (diff > 0) return String.format("<tspan class='diff-plus'>(+%d)</tspan>", diff);
        if (diff < 0) return String.format("<tspan class='diff-minus'>(%d)</tspan>", diff);

        return "";
    }

    private String getTierBackgroundColor(Tier tier) {
        return switch (tier) {
            case DIAMOND -> "#0d1117";
            case PLATINUM -> "#0d1117";
            case GOLD -> "#0d1117";
            case SILVER -> "#0d1117";
            case BRONZE -> "#0d1117";
            case IRON -> "#0d1117";
        };
    }

    private String getTierTextColor(Tier tier) {
        return switch (tier) {
            case DIAMOND -> "#00d4ff"; // Cyan
            case PLATINUM -> "#00ced1"; // Dark Turquoise
            case GOLD -> "#ffd700"; // Gold
            case SILVER -> "#c0c0c0"; // Silver
            case BRONZE -> "#cd7f32"; // Bronze
            case IRON -> "#5f5f5f"; // Gray
        };
    }
}
