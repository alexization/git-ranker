package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.GitHubDataMapper;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRefreshService {

    private final UserRepository userRepository;
    private final UserPersistenceService userPersistenceService;
    private final ActivityLogService activityLogService;
    private final GitHubActivityService gitHubActivityService;
    private final GitHubDataMapper gitHubDataMapper;

    public RegisterUserResponse refresh(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        if (!user.canTriggerFullScan()) {
            throw new BusinessException(ErrorType.REFRESH_COOL_DOWN_EXCEEDED);
        }

        int oldScore = user.getTotalScore();

        GitHubAllActivitiesResponse rawResponse = gitHubActivityService
                .fetchRawAllActivities(username, user.getGithubCreatedAt());

        ActivityStatistics totalStats = gitHubDataMapper.toActivityStatistics(rawResponse);
        ActivityStatistics baselineStats = calculateBaselineStats(user, rawResponse);

        User updatedUser = userPersistenceService.updateUserStatisticsWithLog(
                user.getId(), totalStats, baselineStats);

        int scoreDiff = updatedUser.getTotalScore() - oldScore;

        LogContext.event(Event.USER_REFRESH_REQUESTED)
                .with("username", updatedUser.getUsername())
                .with("old_score", oldScore)
                .with("new_score", updatedUser.getTotalScore())
                .with("score_diff", scoreDiff >= 0 ? "+" + scoreDiff : String.valueOf(scoreDiff))
                .info();

        return createResponse(updatedUser);
    }

    private ActivityStatistics calculateBaselineStats(User user, GitHubAllActivitiesResponse rawResponse) {
        int currentYear = LocalDate.now().getYear();

        if (user.getGithubCreatedAt().getYear() < currentYear) {
            int lastYear = currentYear - 1;
            return gitHubDataMapper.calculateStatisticsUntilYear(rawResponse, lastYear);
        }

        return null;
    }

    private RegisterUserResponse createResponse(User user) {
        ActivityLog activityLog = activityLogService.getLatestLog(user);

        return RegisterUserResponse.of(user, activityLog, false);
    }
}
