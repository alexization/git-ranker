package com.gitranker.api.batch.processor;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.aop.LogExecutionTime;
import com.gitranker.api.global.exception.BusinessException;
import com.gitranker.api.global.exception.ErrorType;
import com.gitranker.api.global.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.exception.GitHubApiRetryableException;
import com.gitranker.api.global.logging.MdcKey;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.GitHubGraphQLClient;
import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreRecalculationProcessor implements ItemProcessor<User, User> {

    private final GitHubGraphQLClient graphQLClient;
    private final GitHubActivityService activityService;
    private final ActivityLogRepository activityLogRepository;

    @Override
    @LogExecutionTime
    public User process(User user) {
        MdcUtils.setUserContext(user.getUsername(), user.getNodeId());

        try {
            int oldScore = user.getTotalScore();
            int currentYear = LocalDate.now().getYear();
            LocalDate startOfThisYear = LocalDate.of(currentYear, 1, 1);

            GitHubActivitySummary finalSummary;

            ActivityLog pastLog =
                    activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(user, startOfThisYear)
                            .orElse(null);

            if (pastLog != null) {
                GitHubAllActivitiesResponse currentYearResponse =
                        graphQLClient.getActivitiesForYear(user.getUsername(), currentYear);

                GitHubActivitySummary currentYearSummary = convertToSummary(currentYearResponse);

                finalSummary = mergeSummary(pastLog, currentYearSummary, currentYearResponse.getMergedPRCount());

                log.info("[Batch] 증분 업데이트 적용 - 사용자: {}", user.getUsername());
            } else {
                finalSummary = activityService.collectAllActivities(user.getUsername(), user.getGithubCreatedAt());

                log.info("[Batch] 전체 업데이트 수행 - 사용자: {}", user.getUsername());
            }

            int newScore = finalSummary.calculateTotalScore();
            user.updateScore(newScore);

            ActivityLog lastLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);
            saveNewActivityLog(user, finalSummary, lastLog);

            log.info("[Domain Event] 점수 갱신 완료 - 사용자: {}, 변동: {}", user.getUsername(), (newScore - oldScore));

            return user;

        } catch (GitHubApiRetryableException | GitHubApiNonRetryableException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Batch Error] 처리 실패 - 사용자: {}", user.getUsername(), e);
            throw new BusinessException(ErrorType.BATCH_STEP_FAILED, "사용자: " + user.getUsername());
        } finally {
            MdcUtils.remove(MdcKey.USERNAME);
            MdcUtils.remove(MdcKey.NODE_ID);
        }
    }

    private GitHubActivitySummary convertToSummary(GitHubAllActivitiesResponse response) {
        return new GitHubActivitySummary(
                response.getCommitCount(),
                response.getPRCount(),
                response.getMergedPRCount(),
                response.getIssueCount(),
                response.getReviewCount()
        );
    }

    private GitHubActivitySummary mergeSummary(ActivityLog past, GitHubActivitySummary current, int totalPrMergedCount) {
        return new GitHubActivitySummary(
                past.getCommitCount() + current.totalCommitCount(),
                past.getPrCount() + current.totalPrOpenedCount(),
                totalPrMergedCount,
                past.getIssueCount() + current.totalIssueCount(),
                past.getReviewCount() + current.totalReviewCount()
        );
    }

    private void saveNewActivityLog(User user, GitHubActivitySummary current, ActivityLog last) {

        int lastCommit = last != null ? last.getCommitCount() : 0;
        int lastIssue = last != null ? last.getIssueCount() : 0;
        int lastPr = last != null ? last.getPrCount() : 0;
        int lastMerged = last != null ? last.getMergedPrCount() : 0;
        int lastReview = last != null ? last.getReviewCount() : 0;

        ActivityLog activityLog = ActivityLog.builder()
                .user(user)
                .activityDate(LocalDate.now())
                .commitCount(current.totalCommitCount())
                .issueCount(current.totalIssueCount())
                .prCount(current.totalPrOpenedCount())
                .mergedPrCount(current.totalPrMergedCount())
                .reviewCount(current.totalReviewCount())
                .diffCommitCount(current.totalCommitCount() - lastCommit)
                .diffIssueCount(current.totalIssueCount() - lastIssue)
                .diffPrCount(current.totalPrOpenedCount() - lastPr)
                .diffMergedPrCount(current.totalPrMergedCount() - lastMerged)
                .diffReviewCount(current.totalReviewCount() - lastReview)
                .build();

        activityLogRepository.save(activityLog);
    }
}
