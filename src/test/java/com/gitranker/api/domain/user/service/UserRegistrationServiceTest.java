package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.GitHubDataMapper;
import com.gitranker.api.infrastructure.github.GitHubGraphQLClient;
import com.gitranker.api.infrastructure.github.dto.GitHubUserInfoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserPersistenceService userPersistenceService;
    @Mock private ActivityLogService activityLogService;
    @Mock private GitHubGraphQLClient gitHubGraphQLClient;
    @Mock private GitHubActivityService gitHubActivityService;
    @Mock private GitHubDataMapper gitHubDataMapper;

    @InjectMocks
    private UserRegistrationService userRegistrationService;

    @Nested
    @DisplayName("기존 사용자 조회")
    class ExistingUser {

        @Test
        @DisplayName("username으로 기존 사용자가 존재하면 GitHub API를 호출하지 않아야 한다.")
        void shouldNotCallGitHubApiForExistingUser() {
            // given
            String username = "existingUser";
            User existingUser = createTestUser(1L, "node123", username);
            ActivityLog activityLog = createTestActivityLog(existingUser);

            given(userRepository.findByUsername(username)).willReturn(Optional.of(existingUser));
            given(activityLogService.findLatestLog(existingUser)).willReturn(Optional.of(activityLog));

            // when
            RegisterUserResponse response = userRegistrationService.register(username);

            // then
            then(gitHubGraphQLClient).shouldHaveNoInteractions();
            then(gitHubActivityService).shouldHaveNoInteractions();
            assertThat(response.isNewUser()).isFalse();
        }
    }

    private User createTestUser(Long id, String nodeId, String username) {
        return User.builder()
                .nodeId(nodeId)
                .username(username)
                .profileImage("https://temp.url")
                .githubCreatedAt(LocalDateTime.now().minusYears(1))
                .build();
    }

    private GitHubUserInfoResponse createGitHubUserInfo(String nodeId, String username) {
        return createGitHubUserInfo(nodeId, username, LocalDateTime.now().minusMonths(6));
    }

    private GitHubUserInfoResponse createGitHubUserInfo(String nodeId, String username, LocalDateTime joinDate) {
        // Mock 또는 실제 객체 생성
        GitHubUserInfoResponse mock = mock(GitHubUserInfoResponse.class);
        given(mock.getNodeId()).willReturn(nodeId);
        given(mock.getLogin()).willReturn(username);
        given(mock.getAvatarUrl()).willReturn("https://avatar.url");
        given(mock.getGitHubCreatedAt()).willReturn(joinDate);
        return mock;
    }

    private ActivityLog createTestActivityLog(User user) {
        return ActivityLog.builder()
                .user(user)
                .activityDate(LocalDate.now())
                .commitCount(100).issueCount(10).prCount(20).mergedPrCount(15).reviewCount(5)
                .diffCommitCount(0).diffIssueCount(0).diffPrCount(0)
                .diffMergedPrCount(0).diffReviewCount(0)
                .build();
    }
}