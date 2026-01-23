package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.auth.OAuthAttributes;
import com.gitranker.api.global.logging.BusinessEventLogger;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.GitHubDataMapper;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final UserPersistenceService userPersistenceService;
    private final ActivityLogService activityLogService;
    private final GitHubActivityService gitHubActivityService;
    private final GitHubDataMapper gitHubDataMapper;
    private final BusinessEventLogger eventLogger;

    public RegisterUserResponse register(OAuthAttributes attributes) {
        String username = attributes.username();
        MdcUtils.setUsername(username);

        Optional<User> existingUser = userRepository.findByNodeId(attributes.nodeId());

        return existingUser.map(user -> handleExistingUser(user, attributes))
                .orElseGet(() -> handleNewUser(attributes));
    }

    private RegisterUserResponse handleNewUser(OAuthAttributes attributes) {
        User newUser = attributes.toEntity();

        GitHubAllActivitiesResponse rawResponse = gitHubActivityService
                .fetchRawAllActivities(newUser.getUsername(), newUser.getGithubCreatedAt());

        ActivityStatistics totalStats = gitHubDataMapper.toActivityStatistics(rawResponse);
        ActivityStatistics baselineStats = calculateBaselineStats(newUser, rawResponse);

        User savedUser = userPersistenceService.saveNewUser(newUser, totalStats, baselineStats);

        eventLogger.userRegistered(savedUser);

        return createResponse(savedUser, true);
    }

    private RegisterUserResponse handleExistingUser(User user, OAuthAttributes attributes) {
        MdcUtils.setNodeId(user.getNodeId());

        boolean isInfoChanged = !user.getUsername().equals(attributes.username()) ||
                                !user.getProfileImage().equals(attributes.profileImage()) ||
                                (attributes.email()) != null && !attributes.email().equals(user.getEmail());

        User currentUser = user;

        if (isInfoChanged) {
            log.debug("사용자 프로필 정보 변경 감지 - 업데이트 수행: 사용자: {}", user.getUsername());
            currentUser = userPersistenceService.updateProfile(user, attributes.username(), attributes.profileImage());
        }

        eventLogger.authSuccess(currentUser.getUsername(), "OAUTH2");

        return createResponse(currentUser, false);
    }

    private ActivityStatistics calculateBaselineStats(User user, GitHubAllActivitiesResponse rawResponse) {
        int currentYear = LocalDate.now().getYear();

        if (user.getGithubCreatedAt().getYear() < currentYear) {
            int lastYear = currentYear - 1;
            return gitHubDataMapper.calculateStatisticsUntilYear(rawResponse, lastYear);
        }

        return null;
    }

    private RegisterUserResponse createResponse(User user, boolean isNewUser) {
        ActivityLog activityLog = activityLogService.findLatestLog(user)
                .orElse(createEmptyActivityLog(user));

        return RegisterUserResponse.of(user, activityLog, isNewUser);
    }

    private ActivityLog createEmptyActivityLog(User user) {
        return ActivityLog.empty(user, LocalDate.now());
    }
}
