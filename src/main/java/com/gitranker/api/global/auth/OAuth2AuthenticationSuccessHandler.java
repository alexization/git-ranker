package com.gitranker.api.global.auth;

import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.domain.user.service.UserRegistrationService;
import com.gitranker.api.global.auth.jwt.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRegistrationService userRegistrationService;
    private final JwtProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String userNameAttributeName = "id";
        OAuthAttributes attributes = OAuthAttributes.of(userNameAttributeName, oAuth2User.getAttributes());

        RegisterUserResponse userResponse = userRegistrationService.register(attributes);

        String jwt = jwtProvider.createToken(userResponse.username(), userResponse.role());

        Cookie jwtCookie = new Cookie("accessToken", jwt);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setMaxAge(60 * 60);
        response.addCookie(jwtCookie);

        getRedirectStrategy().sendRedirect(request, response, "/dashboard");
    }
}
