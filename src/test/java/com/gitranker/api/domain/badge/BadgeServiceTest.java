package com.gitranker.api.domain.badge;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.user.Role;
import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.metrics.BusinessMetrics;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @InjectMocks
    private BadgeService badgeService;

    @Mock private UserRepository userRepository;
    @Mock private ActivityLogRepository activityLogRepository;
    @Mock private SvgBadgeRenderer svgBadgeRenderer;
    @Mock private BusinessMetrics businessMetrics;

    @Test
    @DisplayName("사용자가 존재하면 SVG 뱃지를 생성한다")
    void should_generateBadge_when_userExists() {
        User user = User.builder()
                .githubId(1L)
                .nodeId("node1")
                .username("testuser")
                .githubCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .role(Role.USER)
                .build();
        ActivityLog activityLog = ActivityLog.empty(user, LocalDate.now());

        when(userRepository.findByNodeId("node1")).thenReturn(Optional.of(user));
        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(activityLog);
        when(svgBadgeRenderer.render(eq(user), eq(Tier.IRON), eq(activityLog))).thenReturn("<svg>badge</svg>");

        String badge = badgeService.generateBadge("node1");

        assertThat(badge).isEqualTo("<svg>badge</svg>");
        verify(businessMetrics).incrementBadgeViews();
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 USER_NOT_FOUND 예외가 발생한다")
    void should_throwUserNotFound_when_nodeIdInvalid() {
        when(userRepository.findByNodeId("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> badgeService.generateBadge("invalid"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorType())
                        .isEqualTo(ErrorType.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("활동 로그가 없으면 빈 로그로 뱃지를 생성한다")
    void should_useEmptyLog_when_noActivityLogExists() {
        User user = User.builder()
                .githubId(1L)
                .nodeId("node1")
                .username("testuser")
                .githubCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .role(Role.USER)
                .build();

        when(userRepository.findByNodeId("node1")).thenReturn(Optional.of(user));
        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
        when(svgBadgeRenderer.render(eq(user), eq(Tier.IRON), any(ActivityLog.class))).thenReturn("<svg/>");

        String badge = badgeService.generateBadge("node1");

        assertThat(badge).isNotNull();
        verify(svgBadgeRenderer).render(eq(user), eq(Tier.IRON), any(ActivityLog.class));
    }

    @Test
    @DisplayName("티어별 미리보기 뱃지를 생성한다")
    void should_generatePreviewBadge_when_tierProvided() {
        when(svgBadgeRenderer.render(any(User.class), eq(Tier.DIAMOND), any(ActivityLog.class)))
                .thenReturn("<svg>diamond</svg>");

        String badge = badgeService.generateBadgeByTier(Tier.DIAMOND);

        assertThat(badge).isEqualTo("<svg>diamond</svg>");
    }
}
