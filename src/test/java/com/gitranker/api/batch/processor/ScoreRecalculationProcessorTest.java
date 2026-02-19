package com.gitranker.api.batch.processor;

import com.gitranker.api.batch.strategy.FullActivityUpdateStrategy;
import com.gitranker.api.batch.strategy.IncrementalActivityUpdateStrategy;
import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.Role;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.dto.GitHubNodeUserResponse;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreRecalculationProcessorTest {

    @InjectMocks
    private ScoreRecalculationProcessor processor;

    @Mock private ActivityLogRepository activityLogRepository;
    @Mock private ActivityLogService activityLogService;
    @Mock private IncrementalActivityUpdateStrategy incrementalStrategy;
    @Mock private FullActivityUpdateStrategy fullStrategy;
    @Mock private GitHubActivityService gitHubActivityService;

    private User createUser() {
        return User.builder()
                .githubId(1L)
                .nodeId("node1")
                .username("testuser")
                .githubCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("이전 기준 로그가 없으면 Full 전략을 선택한다")
    void should_selectFullStrategy_when_noBaselineLog() {
        User user = createUser();
        ActivityStatistics stats = ActivityStatistics.of(10, 2, 1, 0, 3);

        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
        when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(eq(user), any()))
                .thenReturn(Optional.empty());
        when(fullStrategy.update(eq(user), any())).thenReturn(stats);

        User result = processor.process(user);

        assertThat(result).isNotNull();
        assertThat(result.getTotalScore()).isGreaterThan(0);
        verify(fullStrategy).update(eq(user), any());
        verify(incrementalStrategy, never()).update(any(), any());
    }

    @Test
    @DisplayName("이전 기준 로그가 있으면 Incremental 전략을 선택한다")
    void should_selectIncrementalStrategy_when_baselineLogExists() {
        User user = createUser();
        ActivityStatistics stats = ActivityStatistics.of(10, 2, 1, 0, 3);
        ActivityLog baselineLog = ActivityLog.empty(user, LocalDate.of(2024, 12, 31));

        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
        when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(eq(user), any()))
                .thenReturn(Optional.of(baselineLog));
        when(incrementalStrategy.update(eq(user), any())).thenReturn(stats);

        User result = processor.process(user);

        assertThat(result).isNotNull();
        verify(incrementalStrategy).update(eq(user), any());
        verify(fullStrategy, never()).update(any(), any());
    }

    @Test
    @DisplayName("GitHubApiRetryableException은 그대로 전파된다")
    void should_rethrowRetryableException() {
        User user = createUser();

        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
        when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(eq(user), any()))
                .thenReturn(Optional.empty());
        when(fullStrategy.update(eq(user), any()))
                .thenThrow(new GitHubApiRetryableException(ErrorType.GITHUB_API_TIMEOUT));

        assertThatThrownBy(() -> processor.process(user))
                .isInstanceOf(GitHubApiRetryableException.class);
    }

    @Test
    @DisplayName("GITHUB_USER_NOT_FOUND가 아닌 NonRetryableException은 그대로 전파된다")
    void should_rethrowNonRetryableException_when_notUserNotFound() {
        User user = createUser();

        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
        when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(eq(user), any()))
                .thenReturn(Optional.empty());
        when(fullStrategy.update(eq(user), any()))
                .thenThrow(new GitHubApiNonRetryableException(ErrorType.GITHUB_COLLECT_ACTIVITY_FAILED));

        assertThatThrownBy(() -> processor.process(user))
                .isInstanceOf(GitHubApiNonRetryableException.class);
    }

    @Test
    @DisplayName("기타 예외는 BusinessException으로 감싸서 전파된다")
    void should_wrapInBusinessException_when_unexpectedError() {
        User user = createUser();

        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
        when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(eq(user), any()))
                .thenReturn(Optional.empty());
        when(fullStrategy.update(eq(user), any()))
                .thenThrow(new RuntimeException("unexpected"));

        assertThatThrownBy(() -> processor.process(user))
                .isInstanceOf(BusinessException.class);
    }

    @Nested
    @DisplayName("username 변경 fallback 테스트")
    class UsernameFallbackTest {

        @Test
        @DisplayName("USER_NOT_FOUND 발생 시 nodeId로 현재 username을 조회하여 프로필 갱신 후 재계산한다")
        void should_retryWithNewUsername_when_userNotFound() {
            User user = createUser();
            ActivityStatistics stats = ActivityStatistics.of(10, 2, 1, 0, 3);

            when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
            when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(eq(user), any()))
                    .thenReturn(Optional.empty());

            // 첫 번째 호출: USER_NOT_FOUND, 두 번째 호출(재시도): 성공
            when(fullStrategy.update(eq(user), any()))
                    .thenThrow(new GitHubApiNonRetryableException(ErrorType.GITHUB_USER_NOT_FOUND))
                    .thenReturn(stats);

            GitHubNodeUserResponse nodeResponse = new GitHubNodeUserResponse(
                    new GitHubNodeUserResponse.Data(
                            new GitHubNodeUserResponse.Node("node1", "newusername", "new@email.com", "https://new-avatar.png"),
                            null
                    )
            );
            when(gitHubActivityService.fetchUserByNodeId("node1")).thenReturn(nodeResponse);

            User result = processor.process(user);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("newusername");
            assertThat(result.getEmail()).isEqualTo("new@email.com");
            assertThat(result.getProfileImage()).isEqualTo("https://new-avatar.png");
            assertThat(result.getTotalScore()).isGreaterThan(0);
            verify(fullStrategy, times(2)).update(eq(user), any());
            verify(gitHubActivityService).fetchUserByNodeId("node1");
        }

        @Test
        @DisplayName("nodeId 조회 결과에 사용자 정보가 없으면 USER_NOT_FOUND 예외를 전파한다")
        void should_throwException_when_nodeIdLookupReturnsNoUser() {
            User user = createUser();

            when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
            when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(eq(user), any()))
                    .thenReturn(Optional.empty());
            when(fullStrategy.update(eq(user), any()))
                    .thenThrow(new GitHubApiNonRetryableException(ErrorType.GITHUB_USER_NOT_FOUND));

            GitHubNodeUserResponse emptyResponse = new GitHubNodeUserResponse(
                    new GitHubNodeUserResponse.Data(null, null)
            );
            when(gitHubActivityService.fetchUserByNodeId("node1")).thenReturn(emptyResponse);

            assertThatThrownBy(() -> processor.process(user))
                    .isInstanceOf(GitHubApiNonRetryableException.class);

            verify(gitHubActivityService).fetchUserByNodeId("node1");
        }

        @Test
        @DisplayName("username 변경 복구 후 재시도에서도 실패하면 예외를 전파한다")
        void should_throwException_when_retryAlsoFails() {
            User user = createUser();

            when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
            when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(eq(user), any()))
                    .thenReturn(Optional.empty());
            when(fullStrategy.update(eq(user), any()))
                    .thenThrow(new GitHubApiNonRetryableException(ErrorType.GITHUB_USER_NOT_FOUND));

            GitHubNodeUserResponse nodeResponse = new GitHubNodeUserResponse(
                    new GitHubNodeUserResponse.Data(
                            new GitHubNodeUserResponse.Node("node1", "newusername", "new@email.com", "https://new-avatar.png"),
                            null
                    )
            );
            when(gitHubActivityService.fetchUserByNodeId("node1")).thenReturn(nodeResponse);

            assertThatThrownBy(() -> processor.process(user))
                    .isInstanceOf(GitHubApiNonRetryableException.class);

            verify(gitHubActivityService).fetchUserByNodeId("node1");
            verify(fullStrategy, times(2)).update(eq(user), any());
        }
    }
}
