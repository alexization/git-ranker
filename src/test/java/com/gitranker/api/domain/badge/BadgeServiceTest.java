package com.gitranker.api.domain.badge;

import com.gitranker.api.domain.log.ActivityLog;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.metrics.BusinessMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.gitranker.api.support.TestFixtures.activityLog;
import static com.gitranker.api.support.TestFixtures.stats;
import static com.gitranker.api.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    private BadgeService badgeService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private SvgBadgeRenderer svgBadgeRenderer;
    @Mock
    private BusinessMetrics businessMetrics;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        badgeService = spy(new BadgeService(
                userRepository,
                activityLogRepository,
                svgBadgeRenderer,
                businessMetrics
        ));
    }

    @Test
    @DisplayName("node id로 사용자를 찾지 못하면 USER_NOT_FOUND 예외가 발생한다")
    void throwsWhenUserDoesNotExist() {
        when(userRepository.findByNodeId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> badgeService.generateBadge("missing"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorType())
                .isEqualTo(ErrorType.USER_NOT_FOUND);

        verifyNoInteractions(svgBadgeRenderer, businessMetrics);
    }

    @Test
    @DisplayName("활동 로그가 있으면 해당 로그로 badge를 렌더링한다")
    void rendersBadgeWithLatestActivityLog() {
        User user = user("alice");
        ActivityLog latestLog = activityLog(user, stats(10, 1, 2, 1, 3), stats(1, 0, 1, 0, 0));

        when(userRepository.findByNodeId("node-alice")).thenReturn(Optional.of(user));
        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(latestLog);
        when(svgBadgeRenderer.render(user, user.getTier(), latestLog)).thenReturn("<svg>badge</svg>");

        String badge = badgeService.generateBadge("node-alice");

        assertThat(badge).isEqualTo("<svg>badge</svg>");
        verify(businessMetrics).incrementBadgeViews();
    }

    @Test
    @DisplayName("활동 로그가 없으면 빈 로그를 만들어 badge를 렌더링한다")
    void fallsBackToEmptyLogWhenActivityLogDoesNotExist() {
        User user = user("alice");
        LocalDate fixedDate = LocalDate.of(2025, 1, 2);

        when(userRepository.findByNodeId("node-alice")).thenReturn(Optional.of(user));
        when(activityLogRepository.getTopByUserOrderByActivityDateDesc(user)).thenReturn(null);
        when(svgBadgeRenderer.render(any(User.class), any(Tier.class), any(ActivityLog.class))).thenReturn("<svg/>");
        doReturn(fixedDate).when(badgeService).currentDate();

        String badge = badgeService.generateBadge("node-alice");

        assertThat(badge).isEqualTo("<svg/>");
        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(svgBadgeRenderer).render(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.eq(user.getTier()), captor.capture());
        assertThat(captor.getValue().getCommitCount()).isZero();
        assertThat(captor.getValue().getActivityDate()).isEqualTo(fixedDate);
    }

    @Test
    @DisplayName("티어 badge 미리보기는 preview 사용자와 활동 통계로 렌더링한다")
    void rendersPreviewBadgeByTier() {
        when(svgBadgeRenderer.render(any(User.class), org.mockito.ArgumentMatchers.eq(Tier.DIAMOND), any(ActivityLog.class)))
                .thenReturn("<svg>diamond</svg>");

        String badge = badgeService.generateBadgeByTier(Tier.DIAMOND);

        assertThat(badge).isEqualTo("<svg>diamond</svg>");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<ActivityLog> logCaptor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(svgBadgeRenderer).render(userCaptor.capture(), org.mockito.ArgumentMatchers.eq(Tier.DIAMOND), logCaptor.capture());

        assertThat(userCaptor.getValue().getUsername()).isEqualTo("DIAMOND");
        assertThat(userCaptor.getValue().getTotalScore()).isEqualTo(12345);
        assertThat(logCaptor.getValue().getMergedPrCount()).isEqualTo(25);
    }
}
