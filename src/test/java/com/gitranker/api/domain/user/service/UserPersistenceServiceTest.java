package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLogOrchestrator;
import com.gitranker.api.domain.ranking.RankingRecalculationService;
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

import static com.gitranker.api.support.TestFixtures.savedUser;
import static com.gitranker.api.support.TestFixtures.stats;
import static com.gitranker.api.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPersistenceServiceTest {

    @InjectMocks
    private UserPersistenceService userPersistenceService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ActivityLogOrchestrator activityLogOrchestrator;
    @Mock
    private RankingRecalculationService rankingRecalculationService;

    @Test
    @DisplayName("신규 사용자를 저장하면 점수와 랭킹을 계산하고 로그와 랭킹 재계산을 수행한다")
    void savesNewUserWithRankingAndLogs() {
        User user = user("new-user");
        ActivityStatistics totalStats = stats(50, 10, 5, 3, 8);
        ActivityStatistics baselineStats = stats(10, 2, 1, 1, 2);

        when(userRepository.countByScoreValueGreaterThan(anyInt())).thenReturn(0L);
        when(userRepository.count()).thenReturn(9L);
        when(userRepository.save(user)).thenReturn(user);

        User savedUser = userPersistenceService.saveNewUser(user, totalStats, baselineStats);

        assertThat(savedUser.getTotalScore()).isEqualTo(totalStats.calculateScore().getValue());
        assertThat(savedUser.getRanking()).isEqualTo(1);
        verify(activityLogOrchestrator).createLogsForNewUser(user, totalStats, baselineStats);
        verify(rankingRecalculationService).recalculateIfNeeded();
    }

    @Test
    @DisplayName("프로필 변경 요청은 사용자 엔티티를 갱신하고 저장한다")
    void updatesProfileAndSavesUser() {
        User user = user("alice");
        LocalDateTime previousUpdatedAt = user.getUpdatedAt();
        when(userRepository.save(user)).thenReturn(user);

        User updatedUser = userPersistenceService.updateProfile(
                user,
                "alice-renamed",
                "https://images.example.com/alice-renamed.png",
                "alice-renamed@example.com"
        );

        assertThat(updatedUser.getUsername()).isEqualTo("alice-renamed");
        assertThat(updatedUser.getEmail()).isEqualTo("alice-renamed@example.com");
        assertThat(updatedUser.getUpdatedAt()).isAfterOrEqualTo(previousUpdatedAt);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("프로필 값이 바뀌지 않아도 현재 구현은 사용자를 다시 저장한다")
    void savesUserEvenWhenProfileValuesAreUnchanged() {
        User user = user("alice");
        LocalDateTime previousUpdatedAt = user.getUpdatedAt();
        when(userRepository.save(user)).thenReturn(user);

        User updatedUser = userPersistenceService.updateProfile(
                user,
                "alice",
                user.getProfileImage(),
                user.getEmail()
        );

        assertThat(updatedUser.getUpdatedAt()).isEqualTo(previousUpdatedAt);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("통계 갱신 대상 사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
    void throwsWhenUserForStatsUpdateDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userPersistenceService.updateUserStatisticsWithLog(
                999L,
                ActivityStatistics.empty(),
                ActivityStatistics.empty()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorType())
                .isEqualTo(ErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 통계를 갱신하면 점수와 로그를 갱신하고 full scan 시각을 기록한다")
    void updatesUserStatisticsAndLogs() {
        User user = savedUser(1L, "alice");
        ActivityStatistics totalStats = stats(80, 12, 9, 6, 15);
        ActivityStatistics baselineStats = stats(30, 3, 4, 2, 5);
        LocalDateTime previousScanTime = user.getLastFullScanAt();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.countByScoreValueGreaterThan(anyInt())).thenReturn(1L);
        when(userRepository.count()).thenReturn(10L);

        User updatedUser = userPersistenceService.updateUserStatisticsWithLog(1L, totalStats, baselineStats);

        assertThat(updatedUser.getTotalScore()).isEqualTo(totalStats.calculateScore().getValue());
        assertThat(updatedUser.getLastFullScanAt()).isAfterOrEqualTo(previousScanTime);
        assertThat(updatedUser.getRanking()).isEqualTo(2);
        verify(activityLogOrchestrator).updateLogsForRefresh(user, totalStats, baselineStats);
        verify(rankingRecalculationService).recalculateIfNeeded();
    }

    @Test
    @DisplayName("baseline 통계가 없어도 사용자 통계 업데이트와 로그 갱신은 수행된다")
    void updatesUserStatisticsWhenBaselineStatsAreNull() {
        User user = savedUser(1L, "alice");
        ActivityStatistics totalStats = stats(20, 2, 1, 0, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.countByScoreValueGreaterThan(anyInt())).thenReturn(5L);
        when(userRepository.count()).thenReturn(20L);

        User updatedUser = userPersistenceService.updateUserStatisticsWithLog(1L, totalStats, null);

        assertThat(updatedUser.getTotalScore()).isEqualTo(totalStats.calculateScore().getValue());
        assertThat(updatedUser.getRanking()).isEqualTo(6);
        verify(activityLogOrchestrator).updateLogsForRefresh(user, totalStats, null);
        verify(rankingRecalculationService).recalculateIfNeeded();
        verifyNoMoreInteractions(rankingRecalculationService);
    }
}
