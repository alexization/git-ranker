package com.gitranker.api.batch.strategy;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncrementalActivityUpdateStrategy implements ActivityUpdateStrategy {

    private final GitHubActivityService activityService;

    @Override
    public ActivityStatistics update(User user, ActivityUpdateContext context) {
        if (!context.hasBaselineLog()) {
            throw new IllegalArgumentException("증분 업데이트에는 baselineLog가 필요합니다.");
        }

        GitHubActivitySummary currentYearSummary = activityService.collectActivityForYear(user.getUsername(), context.currentYear());

        ActivityStatistics mergedStats = mergeWithBaseline(context.baselineLog(), currentYearSummary);

        log.info("증분 업데이트 완료 - 사용자: {}", user.getUsername());

        return mergedStats;
    }

    private ActivityStatistics mergeWithBaseline(ActivityLog baseline, GitHubActivitySummary currentYear) {
        return ActivityStatistics.of(
                baseline.getCommitCount() + currentYear.totalCommitCount(),
                baseline.getIssueCount() + currentYear.totalIssueCount(),
                baseline.getPrCount() + currentYear.totalPrOpenedCount(),
                currentYear.totalPrMergedCount(),
                baseline.getReviewCount() + currentYear.totalReviewCount()
        );
    }
}
