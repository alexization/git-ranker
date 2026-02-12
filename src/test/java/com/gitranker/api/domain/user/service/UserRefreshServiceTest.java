package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.Role;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRefreshServiceTest {

    @InjectMocks
    private UserRefreshService userRefreshService;

    @Mock private UserRepository userRepository;
    @Mock private UserPersistenceService userPersistenceService;
    @Mock private ActivityLogService activityLogService;
    @Mock private GitHubActivityService gitHubActivityService;
    @Mock private GitHubDataMapper gitHubDataMapper;
    @Mock private BaselineStatsCalculator baselineStatsCalculator;
    @Mock private BusinessMetrics businessMetrics;

    private User createUserWithCooldownExpired() {
        User user = User.builder()
                .githubId(1L)
                .nodeId("node1")
                .username("testuser")
                .email("test@test.com")
                .profileImage("https://img.com/1")
                .githubCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .role(Role.USER)
                .build();
        // 쿨다운을 통과시키기 위해 recordFullScan 시간을 과거로 맞출 수 없으므로
        // canTriggerFullScan이 false를 반환하는 것은 별도 테스트에서 검증
        return user;
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 새로고침하면 USER_NOT_FOUND 예외가 발생한다")
    void should_throwUserNotFound_when_usernameDoesNotExist() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userRefreshService.refresh("unknown"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorType())
                        .isEqualTo(ErrorType.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("쿨다운 중이면 REFRESH_COOL_DOWN_EXCEEDED 예외가 발생한다")
    void should_throwCooldownExceeded_when_refreshedRecently() {
        // 방금 생성된 User는 lastFullScanAt=now()이므로 쿨다운 상태
        User user = createUserWithCooldownExpired();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userRefreshService.refresh("testuser"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorType())
                        .isEqualTo(ErrorType.REFRESH_COOL_DOWN_EXCEEDED));

        verify(gitHubActivityService, never()).fetchRawAllActivities(anyString(), any());
    }

    @Test
    @DisplayName("새로고침 성공 시 GitHub API를 호출하고 응답을 반환한다")
    void should_fetchGitHubDataAndReturnResponse_when_cooldownPassed() {
        User user = mock(User.class);
        when(user.canTriggerFullScan()).thenReturn(true);
        when(user.getGithubCreatedAt()).thenReturn(LocalDateTime.of(2020, 1, 1, 0, 0));
        when(user.getId()).thenReturn(1L);
        when(user.getTotalScore()).thenReturn(0);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        GitHubAllActivitiesResponse rawResponse = mock(GitHubAllActivitiesResponse.class);
        when(gitHubActivityService.fetchRawAllActivities(eq("testuser"), any())).thenReturn(rawResponse);

        ActivityStatistics totalStats = ActivityStatistics.of(50, 10, 5, 3, 8);
        ActivityStatistics baselineStats = ActivityStatistics.of(10, 2, 1, 0, 2);
        when(gitHubDataMapper.toActivityStatistics(rawResponse)).thenReturn(totalStats);
        when(baselineStatsCalculator.calculate(user, rawResponse)).thenReturn(baselineStats);

        User updatedUser = mock(User.class);
        when(updatedUser.getUsername()).thenReturn("testuser");
        when(updatedUser.getTotalScore()).thenReturn(100);
        when(updatedUser.getTier()).thenReturn(com.gitranker.api.domain.user.Tier.IRON);
        when(updatedUser.getRanking()).thenReturn(1);
        when(updatedUser.getPercentile()).thenReturn(50.0);
        when(updatedUser.getRole()).thenReturn(Role.USER);

        when(userPersistenceService.updateUserStatisticsWithLog(eq(1L), eq(totalStats), eq(baselineStats)))
                .thenReturn(updatedUser);

        ActivityLog activityLog = ActivityLog.empty(updatedUser, LocalDate.now());
        when(activityLogService.getLatestLog(updatedUser)).thenReturn(activityLog);

        RegisterUserResponse response = userRefreshService.refresh("testuser");

        assertThat(response).isNotNull();
        verify(gitHubActivityService).fetchRawAllActivities(eq("testuser"), any());
        verify(userPersistenceService).updateUserStatisticsWithLog(eq(1L), eq(totalStats), eq(baselineStats));
        verify(businessMetrics).incrementRefreshes();
    }
}
