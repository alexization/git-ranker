package com.gitranker.api.batch.processor;

import com.gitranker.api.batch.strategy.ActivityUpdateContext;
import com.gitranker.api.batch.strategy.ActivityUpdateStrategy;
import com.gitranker.api.batch.strategy.FullActivityUpdateStrategy;
import com.gitranker.api.batch.strategy.IncrementalActivityUpdateStrategy;
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
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.dto.GitHubNodeUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreRecalculationProcessor implements ItemProcessor<User, User> {

    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogService activityLogService;
    private final IncrementalActivityUpdateStrategy incrementalStrategy;
    private final FullActivityUpdateStrategy fullStrategy;
    private final GitHubActivityService gitHubActivityService;

    @Override
    public User process(User user) {
        try {
            return recalculateScore(user);
        } catch (GitHubApiNonRetryableException e) {
            if (e.getErrorType() == ErrorType.GITHUB_USER_NOT_FOUND) {
                return handleUsernameChanged(user);
            }
            throw e;
        } catch (GitHubApiRetryableException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorType.BATCH_STEP_FAILED, "사용자: " + user.getUsername());
        }
    }

    private User recalculateScore(User user) {
        int oldScore = user.getTotalScore();
        int currentYear = LocalDate.now().getYear();

        ActivityStatistics previousStats = findPreviousStats(user);
        ActivityStatistics updateStats = executeUpdate(user, currentYear);

        Score newScore = updateStats.calculateScore();
        user.updateScore(newScore);

        ActivityStatistics diffStats = updateStats.calculateDiff(previousStats);
        activityLogService.saveActivityLog(user, updateStats, diffStats, LocalDate.now());

        log.debug("점수 갱신 완료 - 사용자: {}, 변동: {}",
                user.getUsername(), newScore.differenceFrom(Score.of(oldScore)));

        return user;
    }

    private User handleUsernameChanged(User user) {
        String oldUsername = user.getUsername();

        GitHubNodeUserResponse response = gitHubActivityService.fetchUserByNodeId(user.getNodeId());
        if (!response.hasUser()) {
            throw new GitHubApiNonRetryableException(ErrorType.GITHUB_USER_NOT_FOUND);
        }

        user.updateProfile(response.getLogin(), response.getAvatarUrl(), null);
        log.info("username 변경 감지 - 기존: {}, 신규: {}, nodeId: {}",
                oldUsername, response.getLogin(), user.getNodeId());

        return recalculateScore(user);
    }

    private ActivityStatistics findPreviousStats(User user) {
        ActivityLog lastLog = activityLogRepository.getTopByUserOrderByActivityDateDesc(user);

        return (lastLog != null) ? lastLog.toStatistics() : ActivityStatistics.empty();
    }

    private ActivityStatistics executeUpdate(User user, int currentYear) {
        LocalDate startOfThisYear = LocalDate.of(currentYear, 1, 1);

        ActivityLog baselineLog = activityLogRepository
                .findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(user, startOfThisYear)
                .orElse(null);

        ActivityUpdateStrategy strategy = selectStrategy(baselineLog);
        ActivityUpdateContext context = createContext(baselineLog, currentYear);

        ActivityStatistics stats = strategy.update(user, context);

        return stats;
    }

    private ActivityUpdateStrategy selectStrategy(ActivityLog baselineLog) {
        return (baselineLog != null) ? incrementalStrategy : fullStrategy;
    }

    private ActivityUpdateContext createContext(ActivityLog baselineLog, int currentYear) {
        return (baselineLog != null)
                ? ActivityUpdateContext.forIncremental(baselineLog, currentYear)
                : ActivityUpdateContext.forFull(currentYear);
    }
}
