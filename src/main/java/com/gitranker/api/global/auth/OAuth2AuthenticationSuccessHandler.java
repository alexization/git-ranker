package com.gitranker.api.global.auth;

import com.gitranker.api.domain.auth.service.RefreshTokenService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.domain.user.service.UserRegistrationService;
import com.gitranker.api.global.auth.jwt.JwtProvider;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.logging.Event;
import com.gitranker.api.global.logging.LogContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRegistrationService userRegistrationService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final AuthCookieManager authCookieManager;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String authorizedRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String userNameAttributeName = "id";
        OAuthAttributes attributes = OAuthAttributes.of(userNameAttributeName, oAuth2User.getAttributes());

        RegisterUserResponse userResponse = userRegistrationService.register(attributes);

        User user = userRepository.findByUsername(userResponse.username())
                .orElseThrow(() -> new BusinessException(ErrorType.USER_NOT_FOUND));

        String accessToken = jwtProvider.createAccessToken(userResponse.username(), userResponse.role());

        String refreshTokenValue = refreshTokenService.issueRefreshToken(user);

        authCookieManager.addAccessTokenCookie(response, accessToken);
        authCookieManager.addRefreshTokenCookie(response, refreshTokenValue);

        LogContext.setAuthContext(userResponse.username());

        LogContext.event(Event.USER_LOGIN)
                .with("username", userResponse.username())
                .info();

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, authorizedRedirectUri);
    }
}
