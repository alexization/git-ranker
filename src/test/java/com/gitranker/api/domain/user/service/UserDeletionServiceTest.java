package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.auth.RefreshTokenRepository;
import com.gitranker.api.domain.failure.BatchFailureLogRepository;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.metrics.BusinessMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static com.gitranker.api.support.TestFixtures.savedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserDeletionServiceTest {

    @InjectMocks
    private UserDeletionService userDeletionService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private BatchFailureLogRepository batchFailureLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BusinessMetrics businessMetrics;

    @Test
    @DisplayName("회원 탈퇴 시 관련 데이터와 refresh token 쿠키를 함께 정리한다")
    void deletesUserDataAndClearsRefreshCookie() {
        User user = savedUser(1L, "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ReflectionTestUtils.setField(userDeletionService, "cookieDomain", ".git-ranker.com");
        ReflectionTestUtils.setField(userDeletionService, "isCookieSecure", true);

        userDeletionService.deleteAccount(user, response);

        verify(refreshTokenRepository).deleteAllByUser(user);
        verify(activityLogRepository).deleteAllByUser(user);
        verify(batchFailureLogRepository).deleteAllByTargetId("alice");
        verify(userRepository).delete(user);
        verify(businessMetrics).incrementDeletions();
        assertThat(response.getHeader("Set-Cookie"))
                .contains("refreshToken=")
                .contains("Max-Age=0")
                .contains("Domain=.git-ranker.com")
                .contains("Path=/")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");
    }

    @Test
    @DisplayName("secure 옵션이 false면 삭제 쿠키에도 Secure 속성이 붙지 않는다")
    void omitsSecureFlagWhenCookieIsNotSecure() {
        User user = savedUser(1L, "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ReflectionTestUtils.setField(userDeletionService, "cookieDomain", "localhost");
        ReflectionTestUtils.setField(userDeletionService, "isCookieSecure", false);

        userDeletionService.deleteAccount(user, response);

        assertThat(response.getHeader("Set-Cookie"))
                .contains("Domain=localhost")
                .doesNotContain("Secure");
    }
}
