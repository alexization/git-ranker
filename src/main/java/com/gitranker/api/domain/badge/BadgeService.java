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
import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import com.gitranker.api.global.metrics.BusinessMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final SvgBadgeRenderer svgBadgeRenderer;
    private final BusinessMetrics businessMetrics;

    @Transactional(readOnly = true)
    public String generateBadge(String nodeId) {
        User user = userRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        ActivityLog activityLog = Optional.ofNullable(
                activityLogRepository.getTopByUserOrderByActivityDateDesc(user)
        ).orElseGet(() -> ActivityLog.empty(user, LocalDate.now()));

        LogContext.event(Event.BADGE_VIEWED)
                .with("target_username", user.getUsername())
                .info();

        businessMetrics.incrementBadgeViews();

        return svgBadgeRenderer.render(user, user.getTier(), activityLog);
    }

    @Transactional(readOnly = true)
    public String generateBadgeByTier(Tier tier) {
        User user = User.builder()
                .nodeId("preview")
                .username(tier.toString())
                .build();
        user.updateScore(Score.of(12345));
        user.updateRankInfo(RankInfo.of(1, 0.1, 12345));

        ActivityLog activityLog = ActivityLog.builder()
                .user(user)
                .commitCount(150)
                .prCount(30)
                .mergedPrCount(25)
                .issueCount(10)
                .reviewCount(45)
                .diffCommitCount(12)
                .diffPrCount(0)
                .diffMergedPrCount(0)
                .diffIssueCount(2)
                .diffReviewCount(8)
                .build();

        return svgBadgeRenderer.render(user, tier, activityLog);
    }
}
