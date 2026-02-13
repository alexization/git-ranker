package com.gitranker.api.domain.ranking;

import com.gitranker.api.domain.ranking.dto.RankingList;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RankingController.class)
@Import({SecurityConfig.class, com.gitranker.api.global.auth.CustomAuthenticationEntryPoint.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000"
})
class RankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private RankingService rankingService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean private TimeUtils timeUtils;
    @MockitoBean private BusinessMetrics businessMetrics;

    private RankingList createEmptyRankingList() {
        return new RankingList(
                Collections.emptyList(),
                new RankingList.PageInfo(0, 20, 0, 0, true, true)
        );
    }

    @Test
    @DisplayName("기본 파라미터로 요청하면 200과 랭킹 리스트를 반환한다")
    void should_return200_when_defaultParams() throws Exception {
        when(rankingService.getRankingList(eq(0), isNull())).thenReturn(createEmptyRankingList());

        mockMvc.perform(get("/api/v1/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.rankings").isArray());
    }

    @Test
    @DisplayName("티어 필터를 지정하면 200을 반환한다")
    void should_return200_when_tierProvided() throws Exception {
        when(rankingService.getRankingList(eq(0), eq(Tier.GOLD))).thenReturn(createEmptyRankingList());

        mockMvc.perform(get("/api/v1/ranking").param("tier", "GOLD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @Test
    @DisplayName("음수 page이면 400을 반환한다")
    void should_return400_when_negativePage() throws Exception {
        mockMvc.perform(get("/api/v1/ranking").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }
}
