package com.gitranker.api.batch.processor;

import com.gitranker.api.batch.listener.GitHubCostListener;
import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.domain.user.vo.Score;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcKey;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreRecalculationProcessor implements ItemProcessor<User, User> {

    private final GitHubActivityService activityService;
    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogService activityLogService;

    private StepExecution stepExecution;

    @BeforeStep
    public void saveStepExecution(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public User process(User user) {
        MdcUtils.setUserContext(user.getUsername(), user.getNodeId());
        MdcUtils.setLogContext(LogCategory.BATCH, EventType.REQUEST);

        try {
            int oldScore = user.getTotalScore();
            int currentYear = LocalDate.now().getYear();
            LocalDate startOfThisYear = LocalDate.of(currentYear, 1, 1);

            ActivityLog lastLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);
            ActivityStatistics previousStats = (lastLog != null)
                    ? lastLog.toStatistics()
                    : ActivityStatistics.empty();

            ActivityStatistics newStats;
            ActivityLog pastLog = activityLogRepository
                    .findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(user, startOfThisYear)
                    .orElse(null);

            if (pastLog != null) {
                GitHubActivitySummary currentYearSummary =
                        activityService.collectActivityForYear(user.getUsername(), currentYear);

                newStats = mergeWithPastLog(pastLog, currentYearSummary);

                log.info("증분 업데이트 적용 - 사용자: {}", user.getUsername());
            } else {
                GitHubAllActivitiesResponse fullResponse =
                        activityService.fetchRawAllActivities(user.getUsername(), user.getGithubCreatedAt());

                newStats = activityService.convertToSummary(fullResponse).toActivityStatistics();

                log.info("전체 업데이트 수행 - 사용자: {}", user.getUsername());
            }

            Score newScore = newStats.calculateScore();
            user.updateScore(newScore);

            ActivityStatistics diffStats = newStats.calculateDiff(previousStats);
            activityLogService.saveActivityLog(user, newStats, diffStats, LocalDate.now());

            int cost = Integer.parseInt(MdcUtils.getGithubApiCost());
            addCostToJobContext(cost);

            MdcUtils.setEventType(EventType.SUCCESS);
            log.info("점수 갱신 완료 - 사용자: {}, 변동: {}",
                    user.getUsername(), newScore.differenceFrom(Score.of(oldScore)));

            return user;

        } catch (GitHubApiRetryableException | GitHubApiNonRetryableException e) {
            throw e;
        } catch (Exception e) {
            MdcUtils.setLogContext(LogCategory.BATCH, EventType.FAILURE);
            MdcUtils.setError(e.getClass().getSimpleName(), e.getMessage());
            log.error("점수 재계산 실패 - 사용자: {}, Reason: {}", user.getUsername(), e.getMessage(), e);

            throw new BusinessException(ErrorType.BATCH_STEP_FAILED, "사용자: " + user.getUsername());
        } finally {
            MdcUtils.remove(MdcKey.USERNAME, MdcKey.NODE_ID);
        }
    }

    private synchronized void addCostToJobContext(int cost) {
        if (stepExecution != null) {
            ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
            int currentCost = jobContext.getInt(GitHubCostListener.TOTAL_COST_KEY, 0);
            jobContext.putInt(GitHubCostListener.TOTAL_COST_KEY, currentCost + cost);
        }
    }

    private ActivityStatistics mergeWithPastLog(ActivityLog past, GitHubActivitySummary current) {
        return ActivityStatistics.of(
                past.getCommitCount() + current.totalCommitCount(),
                past.getIssueCount() + current.totalIssueCount(),
                past.getPrCount() + current.totalPrOpenedCount(),
                current.totalPrMergedCount(),
                past.getReviewCount() + current.totalReviewCount()
        );
    }
}
