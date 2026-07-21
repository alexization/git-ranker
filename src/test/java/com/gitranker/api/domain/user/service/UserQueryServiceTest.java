package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.dto.PublicUserResponse;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.metrics.BusinessMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.gitranker.api.support.TestFixtures.activityLog;
import static com.gitranker.api.support.TestFixtures.savedUser;
import static com.gitranker.api.support.TestFixtures.stats;
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
    @DisplayName("사용자가 존재하면 최신 활동 로그를 포함한 공개 응답을 반환한다")
    void returnsPublicProfileWithLatestLog() {
        User user = savedUser(1L, "alice");
        // 카운트마다 서로 다른 값을 주어 필드 매핑이 뒤섞이지 않았는지까지 검증한다.
        ActivityLog activityLog = activityLog(user, stats(11, 12, 13, 14, 15), stats(1, 2, 3, 4, 5));

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(activityLogService.getLatestLog(user)).thenReturn(activityLog);

        PublicUserResponse response = userQueryService.findByUsername("alice");

        assertThat(response.nodeId()).isEqualTo("node-alice");
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.profileImage()).isEqualTo("https://images.example.com/alice.png");
        assertThat(response.updatedAt()).isEqualTo(user.getUpdatedAt());
        assertThat(response.lastFullScanAt()).isEqualTo(user.getLastFullScanAt());
        assertThat(response.totalScore()).isEqualTo(user.getTotalScore());
        assertThat(response.ranking()).isEqualTo(user.getRanking());
        assertThat(response.tier()).isEqualTo(user.getTier());
        assertThat(response.percentile()).isEqualTo(user.getPercentile());
        assertThat(response.commitCount()).isEqualTo(11);
        assertThat(response.issueCount()).isEqualTo(12);
        assertThat(response.prCount()).isEqualTo(13);
        assertThat(response.mergedPrCount()).isEqualTo(14);
        assertThat(response.reviewCount()).isEqualTo(15);
        assertThat(response.diffCommitCount()).isEqualTo(1);
        assertThat(response.diffIssueCount()).isEqualTo(2);
        assertThat(response.diffPrCount()).isEqualTo(3);
        assertThat(response.diffMergedPrCount()).isEqualTo(4);
        assertThat(response.diffReviewCount()).isEqualTo(5);
        verify(businessMetrics).incrementProfileViews();
    }

    @Test
    @DisplayName("공개 응답 DTO에는 PII·내부 식별자 필드가 없고 계약상 19개 공개 필드만 포함한다")
    void publicResponseExcludesPii() {
        Set<String> componentNames = Arrays.stream(PublicUserResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertThat(componentNames).doesNotContain("userId", "githubId", "email", "role", "isNewUser");
        assertThat(componentNames).containsExactlyInAnyOrder(
                "nodeId", "username", "profileImage", "updatedAt", "lastFullScanAt",
                "totalScore", "ranking", "tier", "percentile",
                "commitCount", "issueCount", "prCount", "mergedPrCount", "reviewCount",
                "diffCommitCount", "diffIssueCount", "diffPrCount", "diffMergedPrCount", "diffReviewCount"
        );
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
