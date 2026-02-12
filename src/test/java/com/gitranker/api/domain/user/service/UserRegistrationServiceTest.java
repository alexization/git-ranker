package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.Role;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.auth.OAuthAttributes;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @InjectMocks
    private UserRegistrationService userRegistrationService;

    @Mock private UserRepository userRepository;
    @Mock private UserPersistenceService userPersistenceService;
    @Mock private ActivityLogService activityLogService;
    @Mock private GitHubActivityService gitHubActivityService;
    @Mock private GitHubDataMapper gitHubDataMapper;
    @Mock private BaselineStatsCalculator baselineStatsCalculator;
    @Mock private BusinessMetrics businessMetrics;

    private OAuthAttributes createOAuthAttributes(String username) {
        Map<String, Object> attrs = Map.of(
                "id", 12345,
                "node_id", "MDQ6VXNlcjEyMzQ1",
                "login", username,
                "email", "test@test.com",
                "avatar_url", "https://img.com/1",
                "created_at", "2020-01-01T00:00:00Z"
        );
        return OAuthAttributes.of("id", attrs);
    }

    @Test
    @DisplayName("신규 사용자이면 GitHub 데이터를 수집하고 저장한다")
    void should_fetchGitHubDataAndSave_when_newUser() {
        OAuthAttributes attributes = createOAuthAttributes("newuser");
        when(userRepository.findByNodeId("MDQ6VXNlcjEyMzQ1")).thenReturn(Optional.empty());

        GitHubAllActivitiesResponse rawResponse = mock(GitHubAllActivitiesResponse.class);
        when(gitHubActivityService.fetchRawAllActivities(eq("newuser"), any())).thenReturn(rawResponse);

        ActivityStatistics totalStats = ActivityStatistics.of(10, 2, 1, 0, 3);
        ActivityStatistics baselineStats = ActivityStatistics.empty();
        when(gitHubDataMapper.toActivityStatistics(rawResponse)).thenReturn(totalStats);
        when(baselineStatsCalculator.calculate(any(User.class), eq(rawResponse))).thenReturn(baselineStats);

        User savedUser = User.builder()
                .githubId(12345L)
                .nodeId("MDQ6VXNlcjEyMzQ1")
                .username("newuser")
                .email("test@test.com")
                .profileImage("https://img.com/1")
                .githubCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .role(Role.USER)
                .build();
        when(userPersistenceService.saveNewUser(any(User.class), eq(totalStats), eq(baselineStats)))
                .thenReturn(savedUser);

        ActivityLog activityLog = ActivityLog.empty(savedUser, LocalDate.now());
        when(activityLogService.findLatestLog(savedUser)).thenReturn(Optional.of(activityLog));

        RegisterUserResponse response = userRegistrationService.register(attributes);

        assertThat(response).isNotNull();
        assertThat(response.isNewUser()).isTrue();
        verify(gitHubActivityService).fetchRawAllActivities(eq("newuser"), any());
        verify(userPersistenceService).saveNewUser(any(User.class), eq(totalStats), eq(baselineStats));
        verify(businessMetrics).incrementRegistrations();
    }

    @Test
    @DisplayName("기존 사용자이면 GitHub 데이터를 수집하지 않고 기존 정보를 반환한다")
    void should_returnExisting_when_userAlreadyExists() {
        OAuthAttributes attributes = createOAuthAttributes("existinguser");

        User existingUser = User.builder()
                .githubId(12345L)
                .nodeId("MDQ6VXNlcjEyMzQ1")
                .username("existinguser")
                .email("test@test.com")
                .profileImage("https://img.com/1")
                .githubCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .role(Role.USER)
                .build();
        when(userRepository.findByNodeId("MDQ6VXNlcjEyMzQ1")).thenReturn(Optional.of(existingUser));

        ActivityLog activityLog = ActivityLog.empty(existingUser, LocalDate.now());
        when(activityLogService.findLatestLog(existingUser)).thenReturn(Optional.of(activityLog));

        RegisterUserResponse response = userRegistrationService.register(attributes);

        assertThat(response).isNotNull();
        assertThat(response.isNewUser()).isFalse();
        verify(gitHubActivityService, never()).fetchRawAllActivities(anyString(), any());
        verify(businessMetrics, never()).incrementRegistrations();
    }

    @Test
    @DisplayName("기존 사용자의 프로필이 변경되었으면 업데이트한다")
    void should_updateProfile_when_existingUserInfoChanged() {
        Map<String, Object> attrs = Map.of(
                "id", 12345,
                "node_id", "MDQ6VXNlcjEyMzQ1",
                "login", "newname",
                "email", "test@test.com",
                "avatar_url", "https://img.com/new",
                "created_at", "2020-01-01T00:00:00Z"
        );
        OAuthAttributes attributes = OAuthAttributes.of("id", attrs);

        User existingUser = User.builder()
                .githubId(12345L)
                .nodeId("MDQ6VXNlcjEyMzQ1")
                .username("oldname")
                .email("test@test.com")
                .profileImage("https://img.com/old")
                .githubCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .role(Role.USER)
                .build();
        when(userRepository.findByNodeId("MDQ6VXNlcjEyMzQ1")).thenReturn(Optional.of(existingUser));

        User updatedUser = User.builder()
                .githubId(12345L)
                .nodeId("MDQ6VXNlcjEyMzQ1")
                .username("newname")
                .email("test@test.com")
                .profileImage("https://img.com/new")
                .githubCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .role(Role.USER)
                .build();
        when(userPersistenceService.updateProfile(existingUser, "newname", "https://img.com/new"))
                .thenReturn(updatedUser);

        ActivityLog activityLog = ActivityLog.empty(updatedUser, LocalDate.now());
        when(activityLogService.findLatestLog(updatedUser)).thenReturn(Optional.of(activityLog));

        RegisterUserResponse response = userRegistrationService.register(attributes);

        assertThat(response.username()).isEqualTo("newname");
        verify(userPersistenceService).updateProfile(existingUser, "newname", "https://img.com/new");
    }
}
