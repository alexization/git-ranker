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
        GitHubActivitySummary currentYearSummary = activityService.fetchActivityForYear(user.getUsername(), context.currentYear());

        ActivityStatistics mergedStats = mergeWithBaseline(context.baselineLog(), currentYearSummary);

        log.info("증분 업데이트 완료 - 사용자: {}", user.getUsername());

        return mergedStats;
    }

    private ActivityStatistics mergeWithBaseline(ActivityLog baseline, GitHubActivitySummary currentYear) {
        return ActivityStatistics.of(
                baseline.getCommitCount() + currentYear.commitCount(),
                baseline.getIssueCount() + currentYear.issueCount(),
                baseline.getPrCount() + currentYear.prOpenedCount(),
                currentYear.prMergedCount(),
                baseline.getReviewCount() + currentYear.reviewCount()
        );
    }
}
