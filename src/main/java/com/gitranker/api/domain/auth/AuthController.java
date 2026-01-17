package com.gitranker.api.domain.auth;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.response.ApiResponse;
import com.gitranker.api.global.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(HttpServletRequest request) {
        String refreshToken = CookieUtils.extractRefreshToken(request);
        TokenResponse tokenResponse = authService.refreshAccessToken(refreshToken);

        return ResponseEntity.ok(ApiResponse.success(tokenResponse));

    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = CookieUtils.extractRefreshToken(request);
        authService.logout(refreshToken, response);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/logout/all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @AuthenticationPrincipal User user,
            HttpServletResponse response
    ) {
        authService.logoutAll(user, response);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
