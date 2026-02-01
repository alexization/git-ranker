package com.gitranker.api.batch.strategy;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncrementalActivityUpdateStrategy implements ActivityUpdateStrategy {

    private final GitHubActivityService activityService;

    /**
     * GitHub API 호출은 트랜잭션 외부에서 실행하여 청크 트랜잭션 유지 시간을 최소화합니다.
     * Propagation.NOT_SUPPORTED: 현재 트랜잭션을 일시 중단하고 API 호출 후 재개합니다.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
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
