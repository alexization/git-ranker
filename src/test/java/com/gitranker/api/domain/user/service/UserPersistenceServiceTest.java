package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLogOrchestrator;
import com.gitranker.api.domain.ranking.RankingRecalculationService;
import com.gitranker.api.domain.user.Role;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPersistenceServiceTest {

    @InjectMocks
    private UserPersistenceService userPersistenceService;

    @Mock private UserRepository userRepository;
    @Mock private ActivityLogOrchestrator activityLogOrchestrator;
    @Mock private RankingRecalculationService rankingRecalculationService;

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
    @DisplayName("신규 사용자 저장 시 랭킹 정보를 계산하고 활동 로그를 생성한다")
    void should_calculateRankingAndCreateLogs_when_savingNewUser() {
        User user = createUser();
        ActivityStatistics totalStats = ActivityStatistics.of(50, 10, 5, 3, 8);
        ActivityStatistics baselineStats = ActivityStatistics.empty();

        when(userRepository.countByScoreValueGreaterThan(anyInt())).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.save(user)).thenReturn(user);

        User result = userPersistenceService.saveNewUser(user, totalStats, baselineStats);

        assertThat(result.getTotalScore()).isGreaterThan(0);
        verify(userRepository).save(user);
        verify(activityLogOrchestrator).createLogsForNewUser(user, totalStats, baselineStats);
        verify(rankingRecalculationService).recalculateIfNeeded();
    }

    @Test
    @DisplayName("통계 업데이트 시 사용자가 존재하지 않으면 예외가 발생한다")
    void should_throwUserNotFound_when_userDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userPersistenceService.updateUserStatisticsWithLog(
                999L, ActivityStatistics.empty(), ActivityStatistics.empty()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorType())
                        .isEqualTo(ErrorType.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("통계 업데이트 시 점수와 랭킹을 갱신하고 fullScan을 기록한다")
    void should_updateStatsAndRecordFullScan_when_userExists() {
        User user = createUser();
        ActivityStatistics totalStats = ActivityStatistics.of(50, 10, 5, 3, 8);
        ActivityStatistics baselineStats = ActivityStatistics.empty();

        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(userRepository.countByScoreValueGreaterThan(anyInt())).thenReturn(0L);
        when(userRepository.count()).thenReturn(1L);

        User result = userPersistenceService.updateUserStatisticsWithLog(
                user.getId(), totalStats, baselineStats);

        assertThat(result.getTotalScore()).isGreaterThan(0);
        verify(activityLogOrchestrator).updateLogsForRefresh(user, totalStats, baselineStats);
        verify(rankingRecalculationService).recalculateIfNeeded();
    }

    @Test
    @DisplayName("프로필 업데이트 후 저장한다")
    void should_saveUser_when_profileUpdated() {
        User user = createUser();
        when(userRepository.save(user)).thenReturn(user);

        User result = userPersistenceService.updateProfile(user, "newname", "https://new.img", "new@email.com");

        assertThat(result.getUsername()).isEqualTo("newname");
        verify(userRepository).save(user);
    }
}
