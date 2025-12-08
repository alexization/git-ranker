package com.gitranker.api.batch.processor;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreRecalculationProcessor implements ItemProcessor<User, User> {
    private final GitHubActivityService activityService;
    private final ActivityLogRepository activityLogRepository;

    @Override
    public User process(User user) {
        try {
            GitHubActivitySummary summary =
                    activityService.collectAllActivities(user.getUsername(), user.getGithubCreatedAt());

            int newScore = summary.calculateTotalScore();
            user.updateScore(newScore);

            ActivityLog lastLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);
            saveNewActivityLog(user, summary, lastLog);

            return user;
        } catch (Exception e) {
            log.error("점수 재계산 실패 - 사용자: {}, 에러: {}", user.getUsername(), e.getMessage());
            return null;
        }
    }

    private void saveNewActivityLog(User user, GitHubActivitySummary current, ActivityLog last) {
        int diffCommit = current.totalCommitCount() - last.getCommitCount();
        int diffIssue = current.totalIssueCount() - last.getIssueCount();
        int diffPrOpen = current.totalPrOpenedCount() - last.getPrCount();
        int diffPrMerged = current.totalPrMergedCount() - last.getMergedPrCount();
        int diffReview = current.totalReviewCount() - last.getReviewCount();

        ActivityLog activityLog = ActivityLog.builder()
                .user(user)
                .activityDate(LocalDate.now())
                .commitCount(current.totalCommitCount())
                .issueCount(current.totalIssueCount())
                .prCount(current.totalPrOpenedCount())
                .mergedPrCount(current.totalPrMergedCount())
                .reviewCount(current.totalReviewCount())
                .diffCommitCount(diffCommit)
                .diffIssueCount(diffIssue)
                .diffPrCount(diffPrOpen)
                .diffMergedPrCount(diffPrMerged)
                .diffReviewCount(diffReview)
                .build();

        activityLogRepository.save(activityLog);
    }
}
