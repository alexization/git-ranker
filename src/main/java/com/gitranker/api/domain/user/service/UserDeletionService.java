package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.auth.RefreshTokenRepository;
import com.gitranker.api.domain.failure.BatchFailureLogRepository;
import com.gitranker.api.domain.log.ActivityLogRepository;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import com.gitranker.api.global.metrics.BusinessMetrics;
import com.gitranker.api.global.util.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDeletionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final ActivityLogRepository activityLogRepository;
    private final BatchFailureLogRepository batchFailureLogRepository;
    private final UserRepository userRepository;
    private final BusinessMetrics businessMetrics;

    @Value("${app.cookie.domain}")
    private String cookieDomain;

    @Value("${app.cookie.secure}")
    private boolean isCookieSecure;

    @Transactional
    public void deleteAccount(User user, HttpServletResponse response) {
        String username = user.getUsername();
        String nodeId = user.getNodeId();

        refreshTokenRepository.deleteAllByUser(user);
        activityLogRepository.deleteAllByUser(user);
        batchFailureLogRepository.deleteAllByTargetId(username);
        userRepository.delete(user);

        clearRefreshTokenCookie(response);

        LogContext.event(Event.USER_DELETED)
                .with("username", username)
                .with("node_id", nodeId)
                .info();

        businessMetrics.incrementDeletions();
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = CookieUtils.createDeleteRefreshTokenCookie(cookieDomain, isCookieSecure);
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
