package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.metrics.BusinessMetrics;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.GitHubDataMapper;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.gitranker.api.support.TestFixtures.emptyActivityLog;
import static com.gitranker.api.support.TestFixtures.savedUser;
import static com.gitranker.api.support.TestFixtures.stats;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRefreshServiceTest {

    @InjectMocks
    private UserRefreshService userRefreshService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPersistenceService userPersistenceService;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private GitHubActivityService gitHubActivityService;
    @Mock
    private GitHubDataMapper gitHubDataMapper;
    @Mock
    private BaselineStatsCalculator baselineStatsCalculator;
    @Mock
    private BusinessMetrics businessMetrics;

    @Test
    @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
    void throwsWhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userRefreshService.refresh("missing"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorType())
                .isEqualTo(ErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("쿨다운 중이면 GitHub 조회 없이 REFRESH_COOL_DOWN_EXCEEDED 예외가 발생한다")
    void throwsWhenRefreshCooldownHasNotExpired() {
        User user = savedUser(1L, "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userRefreshService.refresh("alice"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorType())
                .isEqualTo(ErrorType.REFRESH_COOL_DOWN_EXCEEDED);

        verify(gitHubActivityService, never()).fetchRawAllActivities(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("쿨다운이 끝났으면 GitHub 데이터를 다시 가져와 사용자 통계를 갱신한다")
    void refreshesUserWhenCooldownHasExpired() {
        User user = savedUser(1L, "alice");
        ReflectionTestUtils.setField(user, "lastFullScanAt", LocalDateTime.of(2020, 1, 1, 0, 0));
        GitHubAllActivitiesResponse rawResponse = GitHubAllActivitiesResponse.empty();
        ActivityStatistics totalStats = stats(30, 4, 5, 2, 7);
        ActivityStatistics baselineStats = stats(12, 1, 2, 1, 3);
        User updatedUser = savedUser(1L, "alice");
        updatedUser.updateActivityStatistics(totalStats, 0L, 10L);
        ActivityLog latestLog = emptyActivityLog(updatedUser);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(gitHubActivityService.fetchRawAllActivities("alice", user.getGithubCreatedAt())).thenReturn(rawResponse);
        when(gitHubDataMapper.toActivityStatistics(rawResponse)).thenReturn(totalStats);
        when(baselineStatsCalculator.calculate(user, rawResponse)).thenReturn(baselineStats);
        when(userPersistenceService.updateUserStatisticsWithLog(1L, totalStats, baselineStats)).thenReturn(updatedUser);
        when(activityLogService.getLatestLog(updatedUser)).thenReturn(latestLog);

        RegisterUserResponse response = userRefreshService.refresh("alice");

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.totalScore()).isEqualTo(updatedUser.getTotalScore());
        assertThat(response.commitCount()).isEqualTo(latestLog.getCommitCount());
        assertThat(response.isNewUser()).isFalse();
        verify(gitHubActivityService).fetchRawAllActivities("alice", user.getGithubCreatedAt());
        verify(userPersistenceService).updateUserStatisticsWithLog(1L, totalStats, baselineStats);
        verify(businessMetrics).incrementRefreshes();
    }
}
