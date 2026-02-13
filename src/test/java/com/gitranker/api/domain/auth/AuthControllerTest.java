package com.gitranker.api.domain.auth;

import com.gitranker.api.domain.auth.service.AuthService;
import com.gitranker.api.domain.user.Role;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.auth.CustomOAuth2UserService;
import com.gitranker.api.global.auth.OAuth2AuthenticationSuccessHandler;
import com.gitranker.api.global.auth.jwt.JwtProvider;
import com.gitranker.api.global.config.SecurityConfig;
import com.gitranker.api.global.metrics.BusinessMetrics;
import com.gitranker.api.global.util.TimeUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, com.gitranker.api.global.auth.CustomAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean private TimeUtils timeUtils;
    @MockitoBean private BusinessMetrics businessMetrics;

    private UsernamePasswordAuthenticationToken createAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
                user, "",
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getKey()))
        );
    }

    private User createTestUser() {
        return User.builder()
                .githubId(1L)
                .nodeId("node-1")
                .username("testuser")
                .email("test@example.com")
                .profileImage("https://example.com/avatar.png")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/auth/me")
    class Me {

        @Test
        @DisplayName("인증된 사용자면 200과 사용자 정보를 반환한다")
        void should_return200_when_authenticated() throws Exception {
            User user = createTestUser();

            mockMvc.perform(get("/api/v1/auth/me")
                            .with(authentication(createAuthentication(user))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.role").value("USER"));
        }

        @Test
        @DisplayName("인증되지 않으면 401을 반환한다")
        void should_return401_when_notAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("유효한 refreshToken 쿠키가 있으면 200을 반환한다")
        void should_return200_when_validRefreshTokenCookie() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new Cookie("refreshToken", "valid-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"));

            verify(authService).refreshAccessToken(eq("valid-token"), any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("인증된 사용자면 200을 반환한다")
        void should_return200_when_authenticated() throws Exception {
            User user = createTestUser();

            mockMvc.perform(post("/api/v1/auth/logout")
                            .with(authentication(createAuthentication(user)))
                            .cookie(new Cookie("refreshToken", "valid-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"));
        }

        @Test
        @DisplayName("인증되지 않으면 401을 반환한다")
        void should_return401_when_notAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout/all")
    class LogoutAll {

        @Test
        @DisplayName("인증된 사용자면 200을 반환한다")
        void should_return200_when_authenticated() throws Exception {
            User user = createTestUser();

            mockMvc.perform(post("/api/v1/auth/logout/all")
                            .with(authentication(createAuthentication(user))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"));
        }

        @Test
        @DisplayName("인증되지 않으면 401을 반환한다")
        void should_return401_when_notAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout/all"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
