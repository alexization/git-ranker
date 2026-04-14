package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.auth.OAuthAttributes;
import com.gitranker.api.global.metrics.BusinessMetrics;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.GitHubDataMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.gitranker.api.support.TestFixtures.emptyActivityLog;
import static com.gitranker.api.support.TestFixtures.oauthAttributes;
import static com.gitranker.api.support.TestFixtures.savedUser;
import static com.gitranker.api.support.TestFixtures.stats;
import static com.gitranker.api.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @InjectMocks
    private UserRegistrationService userRegistrationService;

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
    @DisplayName("신규 사용자면 GitHub 활동을 수집해 저장하고 신규 응답을 반환한다")
    void registersNewUser() {
        OAuthAttributes attributes = oauthAttributes("new-user", "new-user@example.com", "https://images.example.com/new-user.png");
        ActivityStatistics totalStats = stats(10, 2, 3, 1, 4);
        ActivityStatistics baselineStats = stats(5, 1, 1, 1, 2);
        User savedUser = savedUser(1L, "new-user");
        ActivityLog latestLog = emptyActivityLog(savedUser);

        when(userRepository.findByNodeId("MDQ6VXNlcjEyMzQ1")).thenReturn(Optional.empty());
        when(gitHubActivityService.fetchRawAllActivities("new-user", attributes.githubCreatedAt())).thenReturn(null);
        when(gitHubDataMapper.toActivityStatistics(null)).thenReturn(totalStats);
        when(baselineStatsCalculator.calculate(any(User.class), org.mockito.ArgumentMatchers.isNull())).thenReturn(baselineStats);
        when(userPersistenceService.saveNewUser(any(User.class), org.mockito.ArgumentMatchers.eq(totalStats), org.mockito.ArgumentMatchers.eq(baselineStats)))
                .thenReturn(savedUser);
        when(activityLogService.findLatestLog(savedUser)).thenReturn(Optional.of(latestLog));

        RegisterUserResponse response = userRegistrationService.register(attributes);

        assertThat(response.username()).isEqualTo("new-user");
        assertThat(response.isNewUser()).isTrue();
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("new-user@example.com");
        assertThat(response.profileImage()).isEqualTo("https://images.example.com/new-user.png");
        assertThat(response.commitCount()).isEqualTo(latestLog.getCommitCount());
        verify(userPersistenceService).saveNewUser(any(User.class), org.mockito.ArgumentMatchers.eq(totalStats), org.mockito.ArgumentMatchers.eq(baselineStats));
        verify(businessMetrics).incrementRegistrations();
    }

    @Test
    @DisplayName("기존 사용자 정보가 바뀌지 않았으면 GitHub를 다시 조회하지 않고 기존 응답을 반환한다")
    void returnsExistingUserWithoutGitHubFetchWhenProfileDidNotChange() {
        OAuthAttributes attributes = oauthAttributes("alice", "alice@example.com", "https://images.example.com/alice.png");
        User existingUser = savedUser(1L, "alice");
        ActivityLog latestLog = emptyActivityLog(existingUser);

        when(userRepository.findByNodeId("MDQ6VXNlcjEyMzQ1")).thenReturn(Optional.of(existingUser));
        when(activityLogService.findLatestLog(existingUser)).thenReturn(Optional.of(latestLog));

        RegisterUserResponse response = userRegistrationService.register(attributes);

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.isNewUser()).isFalse();
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.profileImage()).isEqualTo("https://images.example.com/alice.png");
        verify(gitHubActivityService, never()).fetchRawAllActivities(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        verify(businessMetrics, never()).incrementRegistrations();
    }

    @Test
    @DisplayName("기존 사용자 프로필이 바뀌었으면 업데이트 후 최신 정보로 응답한다")
    void updatesExistingUserProfileWhenChanged() {
        OAuthAttributes attributes = oauthAttributes("alice-renamed", "alice@example.com", "https://images.example.com/alice-renamed.png");
        User existingUser = user("alice");
        User updatedUser = savedUser(1L, "alice-renamed");
        ActivityLog latestLog = emptyActivityLog(updatedUser);

        when(userRepository.findByNodeId("MDQ6VXNlcjEyMzQ1")).thenReturn(Optional.of(existingUser));
        when(userPersistenceService.updateProfile(
                existingUser,
                "alice-renamed",
                "https://images.example.com/alice-renamed.png",
                "alice@example.com"
        )).thenReturn(updatedUser);
        when(activityLogService.findLatestLog(updatedUser)).thenReturn(Optional.of(latestLog));

        RegisterUserResponse response = userRegistrationService.register(attributes);

        assertThat(response.username()).isEqualTo("alice-renamed");
        assertThat(response.profileImage()).isEqualTo("https://images.example.com/alice-renamed.png");
        assertThat(response.email()).isEqualTo(updatedUser.getEmail());
        verify(userPersistenceService).updateProfile(
                existingUser,
                "alice-renamed",
                "https://images.example.com/alice-renamed.png",
                "alice@example.com"
        );
    }
}
