package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.metrics.BusinessMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.gitranker.api.support.TestFixtures.emptyActivityLog;
import static com.gitranker.api.support.TestFixtures.savedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @InjectMocks
    private UserQueryService userQueryService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private BusinessMetrics businessMetrics;

    @Test
    @DisplayName("사용자가 존재하면 최신 활동 로그를 포함한 응답을 반환한다")
    void returnsUserProfileWithLatestLog() {
        User user = savedUser(1L, "alice");
        user.updateProfile("alice", "https://images.example.com/alice-profile.png", "alice@example.com");
        ActivityLog activityLog = emptyActivityLog(user);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(activityLogService.getLatestLog(user)).thenReturn(activityLog);

        RegisterUserResponse response = userQueryService.findByUsername("alice");

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.profileImage()).isEqualTo("https://images.example.com/alice-profile.png");
        assertThat(response.role()).isEqualTo(user.getRole());
        assertThat(response.commitCount()).isEqualTo(activityLog.getCommitCount());
        assertThat(response.diffCommitCount()).isEqualTo(activityLog.getDiffCommitCount());
        assertThat(response.isNewUser()).isFalse();
        verify(businessMetrics).incrementProfileViews();
    }

    @Test
    @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
    void throwsWhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.findByUsername("missing"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorType())
                .isEqualTo(ErrorType.USER_NOT_FOUND);

        verifyNoInteractions(activityLogService, businessMetrics);
    }

    @Test
    @DisplayName("최신 로그 조회가 실패하면 예외를 그대로 전파하고 조회 metric은 증가하지 않는다")
    void propagatesActivityLogLookupFailureWithoutIncrementingMetrics() {
        User user = savedUser(1L, "alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(activityLogService.getLatestLog(user)).thenThrow(new BusinessException(ErrorType.ACTIVITY_LOG_NOT_FOUND));

        assertThatThrownBy(() -> userQueryService.findByUsername("alice"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorType())
                .isEqualTo(ErrorType.ACTIVITY_LOG_NOT_FOUND);

        verifyNoInteractions(businessMetrics);
    }
}
