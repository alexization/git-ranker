package com.gitranker.api.domain.badge;

import com.gitranker.api.domain.user.Tier;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.auth.CustomOAuth2UserService;
import com.gitranker.api.global.auth.OAuth2AuthenticationSuccessHandler;
import com.gitranker.api.global.auth.jwt.JwtProvider;
import com.gitranker.api.global.config.SecurityConfig;
import com.gitranker.api.global.metrics.BusinessMetrics;
import com.gitranker.api.global.util.TimeUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BadgeController.class)
@Import({SecurityConfig.class, com.gitranker.api.global.auth.CustomAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000"
})
class BadgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private BadgeService badgeService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean private TimeUtils timeUtils;
    @MockitoBean private BusinessMetrics businessMetrics;

    @Test
    @DisplayName("nodeId로 뱃지를 요청하면 SVG를 반환한다")
    void should_returnSvg_when_validNodeId() throws Exception {
        String svgContent = "<svg>test badge</svg>";
        when(badgeService.generateBadge("node-123")).thenReturn(svgContent);

        mockMvc.perform(get("/api/v1/badges/node-123"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(content().string(svgContent))
                .andExpect(header().exists("Cache-Control"));
    }

    @Test
    @DisplayName("티어별 뱃지를 요청하면 SVG를 반환한다")
    void should_returnSvg_when_validTier() throws Exception {
        String svgContent = "<svg>gold badge</svg>";
        when(badgeService.generateBadgeByTier(Tier.GOLD)).thenReturn(svgContent);

        mockMvc.perform(get("/api/v1/badges/GOLD/badge"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(content().string(svgContent));
    }
}
