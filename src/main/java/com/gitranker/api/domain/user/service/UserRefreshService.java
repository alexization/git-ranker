package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.GitHubDataMapper;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        MdcUtils.setLogContext(LogCategory.DOMAIN, EventType.REQUEST);
        MdcUtils.setUsername(username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        MdcUtils.setNodeId(user.getNodeId());

        if (!user.canTriggerFullScan()) {
            MdcUtils.setEventType(EventType.FAILURE);
            log.info("갱신 쿨다운 미충족 - 사용자: {}, 다음 가능 시간: {}",
                    username, user.getNextFullScanAvailableAt());

            throw new BusinessException(ErrorType.REFRESH_COOL_DOWN_EXCEEDED);
        }

        log.debug("수동 전체 갱신 시작 - 사용자: {}", username);

        GitHubAllActivitiesResponse rawResponse = gitHubActivityService
                .fetchRawAllActivities(username, user.getGithubCreatedAt());

        ActivityStatistics totalStats = gitHubDataMapper.toActivityStatistics(rawResponse);

        User updatedUser = userPersistenceService.updateUserStatistics(user.getId(), totalStats);

        MdcUtils.setEventType(EventType.SUCCESS);
        log.info("수동 전체 갱신 완료 - 사용자: {}, 신규 점수: {}", username, updatedUser.getTotalScore());

        return createResponse(updatedUser);
    }

    private RegisterUserResponse createResponse(User user) {
        ActivityLog activityLog = activityLogService.getLatestLog(user);

        return RegisterUserResponse.of(user, activityLog, false);
    }
}
