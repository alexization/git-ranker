package com.gitranker.api.domain.badge;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.vo.RankInfo;
import com.gitranker.api.domain.user.vo.Score;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private static final String GITHUB_LOGO_PATH = "M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.137 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z";
    private static final String FONT_IMPORT_CSS = "@import url('https://fonts.googleapis.com/css2?&family=Noto+Sans+KR:wght@400;500;700&family=Outfit:wght@400;500;700;900&display=swap');";

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;

    @Transactional(readOnly = true)
    public String generateBadge(String nodeId) {
        User user = userRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        ActivityLog activityLog = Optional.ofNullable(
                activityLogRepository.getTopByUserOrderByActivityDateDesc(user)
        ).orElseGet(() -> ActivityLog.empty(user, LocalDate.now()));

        MdcUtils.setUserContext(user.getUsername(), nodeId);
        MdcUtils.setLogContext(LogCategory.DOMAIN, EventType.SUCCESS);

        return createSvgContent(user, user.getTier(), activityLog);
    }

    @Transactional(readOnly = true)
    public String generateBadgeByTier(Tier tier) {
        User user = User.builder().nodeId("preview").username(tier.toString()).build();
        user.updateScore(Score.of(12345));
        user.updateRankInfo(RankInfo.of(1, 0.1, 12345));

        ActivityLog activityLog = ActivityLog.builder()
                .user(user).commitCount(150).prCount(30).mergedPrCount(25).issueCount(10).reviewCount(45)
                .diffCommitCount(12).diffPrCount(0).diffMergedPrCount(0).diffIssueCount(2).diffReviewCount(8).build();
        return createSvgContent(user, tier, activityLog);
    }

    private String createSvgContent(User user, Tier tier, ActivityLog activityLog) {
        String gradientDefs = getTierGradientDefs(tier);

        String displayTierName = tier.name().charAt(0) + tier.name().substring(1).toLowerCase();

        int tierFontSize = 32;
        if (displayTierName.length() > 9) {
            tierFontSize = 26;
        } else if (displayTierName.length() > 6) {
            tierFontSize = 30;
        }

        return String.format("""
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
                        """,
                gradientDefs,
                FONT_IMPORT_CSS,
                tierFontSize,
                GITHUB_LOGO_PATH,
                user.getUsername(),
                displayTierName,
                formatNumber(user.getTotalScore()),
                user.getPercentile(),
                formatNumber(user.getRanking()),

                formatCount(activityLog.getCommitCount()), formatDiff(activityLog.getDiffCommitCount()),
                formatCount(activityLog.getIssueCount()), formatDiff(activityLog.getDiffIssueCount()),
                formatCount(activityLog.getPrCount()), formatDiff(activityLog.getDiffPrCount()),
                formatCount(activityLog.getMergedPrCount()), formatDiff(activityLog.getDiffMergedPrCount()),
                formatCount(activityLog.getReviewCount()), formatDiff(activityLog.getDiffReviewCount())
        );
    }

    private String formatNumber(long number) {
        return String.format("%,d", number);
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
                color1 = "#09203F";
                color2 = "#3B82F6";
                color3 = "#D4AF37";
            }
            case MASTER -> {
                color1 = "#2E1065";
                color2 = "#7C3AED";
                color3 = "#F472B6";
            }
            case DIAMOND -> {
                color1 = "#0C4A6E";
                color2 = "#0284C7";
                color3 = "#7DD3FC";
            }
            case EMERALD -> {
                color1 = "#064E3B";
                color2 = "#059669";
                color3 = "#34D399";
            }
            case PLATINUM -> {
                color1 = "#1E293B";
                color2 = "#0F766E";
                color3 = "#2DD4BF";
            }
            case GOLD -> {
                color1 = "#8E6310";
                color2 = "#C2971F";
                color3 = "#F4D03F";
            }
            case SILVER -> {
                color1 = "#111827";
                color2 = "#4B5563";
                color3 = "#9CA3AF";
            }
            case BRONZE -> {
                color1 = "#431407";
                color2 = "#92400E";
                color3 = "#D97706";
            }
            default -> {
                color1 = "#0F172A";
                color2 = "#334155";
                color3 = "#64748B";
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