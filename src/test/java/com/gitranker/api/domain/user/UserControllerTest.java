package com.gitranker.api.domain.user;

import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.domain.user.service.UserDeletionService;
import com.gitranker.api.domain.user.service.UserQueryService;
import com.gitranker.api.domain.user.service.UserRefreshService;
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

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, com.gitranker.api.global.auth.CustomAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserQueryService userQueryService;
    @MockitoBean private UserRefreshService userRefreshService;
    @MockitoBean private UserDeletionService userDeletionService;
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

    private User createTestUser(String username) {
        return User.builder()
                .githubId(1L)
                .nodeId("node-1")
                .username(username)
                .role(Role.USER)
                .build();
    }

    private RegisterUserResponse createResponse(String username) {
        return new RegisterUserResponse(
                1L, 1L, "node-1", username, null, null, Role.USER,
                LocalDateTime.now(), LocalDateTime.now(),
                1000, 1, Tier.SILVER, 50.0,
                100, 10, 5, 3, 8,
                10, 1, 1, 0, 2,
                false
        );
    }

    @Nested
    @DisplayName("GET /api/v1/users/{username}")
    class GetUser {

        @Test
        @DisplayName("유효한 username이면 200과 사용자 정보를 반환한다")
        void should_return200_when_validUsername() throws Exception {
            when(userQueryService.findByUsername("testuser")).thenReturn(createResponse("testuser"));

            mockMvc.perform(get("/api/v1/users/testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }

        @Test
        @DisplayName("허용되지 않는 username 형식이면 400을 반환한다")
        void should_return400_when_invalidUsernameFormat() throws Exception {
            mockMvc.perform(get("/api/v1/users/invalid--user"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/{username}/refresh")
    class RefreshUser {

        @Test
        @DisplayName("본인 계정이면 200과 갱신된 정보를 반환한다")
        void should_return200_when_sameUser() throws Exception {
            User user = createTestUser("testuser");
            when(userRefreshService.refresh("testuser")).thenReturn(createResponse("testuser"));

            mockMvc.perform(post("/api/v1/users/testuser/refresh")
                            .with(authentication(createAuthentication(user))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"));

            verify(userRefreshService).refresh("testuser");
        }

        @Test
        @DisplayName("다른 사용자 계정이면 403을 반환한다")
        void should_return403_when_differentUser() throws Exception {
            User user = createTestUser("otheruser");

            mockMvc.perform(post("/api/v1/users/testuser/refresh")
                            .with(authentication(createAuthentication(user))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증되지 않으면 401을 반환한다")
        void should_return401_when_notAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/users/testuser/refresh"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/me")
    class DeleteMyAccount {

        @Test
        @DisplayName("인증된 사용자면 204를 반환한다")
        void should_return204_when_authenticated() throws Exception {
            User user = createTestUser("testuser");

            mockMvc.perform(delete("/api/v1/users/me")
                            .with(authentication(createAuthentication(user))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("인증되지 않으면 401을 반환한다")
        void should_return401_when_notAuthenticated() throws Exception {
            mockMvc.perform(delete("/api/v1/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
