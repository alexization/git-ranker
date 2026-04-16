package com.gitranker.api.batch.processor;

import com.gitranker.api.batch.strategy.ActivityUpdateContext;
import com.gitranker.api.batch.strategy.FullActivityUpdateStrategy;
import com.gitranker.api.batch.strategy.IncrementalActivityUpdateStrategy;
import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import com.gitranker.api.global.logging.LogSanitizer;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.dto.GitHubNodeUserResponse;
import com.gitranker.api.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoreRecalculationProcessorTest {

    @InjectMocks
    private ScoreRecalculationProcessor processor;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private IncrementalActivityUpdateStrategy incrementalStrategy;

    @Mock
    private FullActivityUpdateStrategy fullStrategy;

    @Mock
    private GitHubActivityService gitHubActivityService;

    @Test
    @DisplayName("processor uses incremental strategy when a baseline log exists")
    void usesIncrementalStrategyWhenBaselineExists() {
        User user = TestFixtures.user("alice");
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        LocalDate currentYearStart = LocalDate.of(currentYear, 1, 1);
        ActivityLog latestLog = ActivityLog.of(
                user,
                TestFixtures.stats(4, 1, 1, 1, 2),
                TestFixtures.stats(1, 0, 0, 0, 1),
                LocalDate.of(2025, 12, 31)
        );
        ActivityLog baselineLog = ActivityLog.baseline(
                user,
                TestFixtures.stats(10, 3, 2, 7, 5),
                LocalDate.of(2025, 1, 1)
        );
        ActivityStatistics updatedStats = TestFixtures.stats(12, 4, 3, 8, 6);

        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(latestLog);
        when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(
                eq(user),
                eq(currentYearStart)
        )).thenReturn(Optional.of(baselineLog));
        when(incrementalStrategy.update(eq(user), any(ActivityUpdateContext.class))).thenReturn(updatedStats);

        User processed = processor.process(user);

        assertThat(processed).isSameAs(user);
        assertThat(user.getTotalScore()).isEqualTo(updatedStats.calculateScore().getValue());
        verify(fullStrategy, never()).update(any(), any());

        ArgumentCaptor<ActivityUpdateContext> contextCaptor = ArgumentCaptor.forClass(ActivityUpdateContext.class);
        verify(incrementalStrategy).update(eq(user), contextCaptor.capture());
        assertThat(contextCaptor.getValue().baselineLog()).isSameAs(baselineLog);
        assertThat(contextCaptor.getValue().currentYear()).isEqualTo(currentYear);

        ArgumentCaptor<ActivityStatistics> diffCaptor = ArgumentCaptor.forClass(ActivityStatistics.class);
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(activityLogService).saveActivityLog(eq(user), eq(updatedStats), diffCaptor.capture(), dateCaptor.capture());
        assertThat(diffCaptor.getValue()).isEqualTo(updatedStats.calculateDiff(latestLog.toStatistics()));
        assertThat(dateCaptor.getValue()).isEqualTo(today);
    }

    @Test
    @DisplayName("processor uses full strategy and empty diff when no previous logs exist")
    void usesFullStrategyWhenNoBaselineExists() {
        User user = TestFixtures.user("alice");
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        LocalDate currentYearStart = LocalDate.of(currentYear, 1, 1);
        ActivityStatistics updatedStats = TestFixtures.stats(3, 2, 1, 4, 5);

        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
        when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(
                eq(user),
                eq(currentYearStart)
        )).thenReturn(Optional.empty());
        when(fullStrategy.update(eq(user), any(ActivityUpdateContext.class))).thenReturn(updatedStats);

        User processed = processor.process(user);

        assertThat(processed).isSameAs(user);
        assertThat(user.getTotalScore()).isEqualTo(updatedStats.calculateScore().getValue());
        verify(incrementalStrategy, never()).update(any(), any());

        ArgumentCaptor<ActivityUpdateContext> contextCaptor = ArgumentCaptor.forClass(ActivityUpdateContext.class);
        verify(fullStrategy).update(eq(user), contextCaptor.capture());
        assertThat(contextCaptor.getValue().baselineLog()).isNull();
        assertThat(contextCaptor.getValue().currentYear()).isEqualTo(currentYear);

        verify(activityLogService).saveActivityLog(
                eq(user),
                eq(updatedStats),
                eq(updatedStats.calculateDiff(ActivityStatistics.empty())),
                eq(today)
        );
    }

    @Test
    @DisplayName("processor refreshes profile by node id and retries when username changed")
    void refreshesProfileAndRetriesWhenUsernameChanged() {
        User user = TestFixtures.user("old-name");
        ActivityStatistics updatedStats = TestFixtures.stats(9, 1, 2, 3, 4);
        GitHubNodeUserResponse response = new GitHubNodeUserResponse(
                new GitHubNodeUserResponse.Data(
                        new GitHubNodeUserResponse.Node("node", "new-name", "new@example.com", "https://images.example.com/new.png"),
                        null
                )
        );

        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
        when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(
                eq(user),
                any(LocalDate.class)
        )).thenReturn(Optional.empty());
        when(fullStrategy.update(eq(user), any(ActivityUpdateContext.class)))
                .thenThrow(new GitHubApiNonRetryableException(ErrorType.GITHUB_USER_NOT_FOUND))
                .thenReturn(updatedStats);
        when(gitHubActivityService.fetchUserByNodeId(user.getNodeId())).thenReturn(response);

        User processed = processor.process(user);

        assertThat(processed).isSameAs(user);
        assertThat(user.getUsername()).isEqualTo("new-name");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getProfileImage()).isEqualTo("https://images.example.com/new.png");
        assertThat(user.getTotalScore()).isEqualTo(updatedStats.calculateScore().getValue());

        verify(gitHubActivityService).fetchUserByNodeId(user.getNodeId());
        verify(fullStrategy, times(2)).update(eq(user), any(ActivityUpdateContext.class));
        verify(activityLogService).saveActivityLog(
                eq(user),
                eq(updatedStats),
                eq(updatedStats),
                eq(LocalDate.now())
        );
    }

    @Test
    @DisplayName("processor keeps user-not-found when node lookup cannot resolve a replacement user")
    void rethrowsUserNotFoundWhenNodeLookupFails() {
        User user = TestFixtures.user("alice");
        GitHubNodeUserResponse response = new GitHubNodeUserResponse(
                new GitHubNodeUserResponse.Data(null, null)
        );

        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
        when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(
                eq(user),
                any(LocalDate.class)
        )).thenReturn(Optional.empty());
        when(fullStrategy.update(eq(user), any(ActivityUpdateContext.class)))
                .thenThrow(new GitHubApiNonRetryableException(ErrorType.GITHUB_USER_NOT_FOUND));
        when(gitHubActivityService.fetchUserByNodeId(user.getNodeId())).thenReturn(response);

        assertThatThrownBy(() -> processor.process(user))
                .isInstanceOf(GitHubApiNonRetryableException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.GITHUB_USER_NOT_FOUND);

        verify(activityLogService, never()).saveActivityLog(any(), any(), any(), any());
    }

    @Test
    @DisplayName("processor propagates retryable GitHub errors without wrapping them")
    void propagatesRetryableGitHubError() {
        User user = TestFixtures.user("alice");
        GitHubApiRetryableException exception = new GitHubApiRetryableException(ErrorType.GITHUB_API_TIMEOUT);

        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
        when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(
                eq(user),
                any(LocalDate.class)
        )).thenReturn(Optional.empty());
        when(fullStrategy.update(eq(user), any(ActivityUpdateContext.class))).thenThrow(exception);

        assertThatThrownBy(() -> processor.process(user))
                .isSameAs(exception);
    }

    @Test
    @DisplayName("processor propagates non-retryable GitHub errors other than username-changed")
    void propagatesNonRetryableGitHubError() {
        User user = TestFixtures.user("alice");
        GitHubApiNonRetryableException exception =
                new GitHubApiNonRetryableException(ErrorType.GITHUB_COLLECT_ACTIVITY_FAILED);

        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
        when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(
                eq(user),
                any(LocalDate.class)
        )).thenReturn(Optional.empty());
        when(fullStrategy.update(eq(user), any(ActivityUpdateContext.class))).thenThrow(exception);

        assertThatThrownBy(() -> processor.process(user))
                .isSameAs(exception);
    }

    @Test
    @DisplayName("processor wraps unexpected failures with batch-step business exception")
    void wrapsUnexpectedFailure() {
        User user = TestFixtures.user("alice");

        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
        when(activityLogRepository.findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(
                eq(user),
                any(LocalDate.class)
        )).thenReturn(Optional.empty());
        when(fullStrategy.update(eq(user), any(ActivityUpdateContext.class)))
                .thenThrow(new IllegalStateException("unexpected"));

        assertThatThrownBy(() -> processor.process(user))
                .isInstanceOf(BusinessException.class)
                .satisfies(throwable -> {
                    BusinessException exception = (BusinessException) throwable;
                    assertThat(exception.getErrorType()).isEqualTo(ErrorType.BATCH_STEP_FAILED);
                    assertThat(exception.getData()).isEqualTo("사용자 해시: " + LogSanitizer.hashUsername(user.getUsername()));
                });

        verify(activityLogService, never()).saveActivityLog(any(), any(), any(), any());
    }
}
