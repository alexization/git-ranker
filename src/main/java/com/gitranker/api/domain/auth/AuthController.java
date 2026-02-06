package com.gitranker.api.domain.auth;

import com.gitranker.api.domain.auth.dto.AuthMeResponse;
import com.gitranker.api.domain.auth.service.AuthService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.response.ApiResponse;
import com.gitranker.api.global.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthMeResponse>> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ErrorType.UNAUTHORIZED_ACCESS));
        }

        return ResponseEntity.ok(ApiResponse.success(AuthMeResponse.from(user)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = CookieUtils.extractRefreshToken(request);
        authService.refreshAccessToken(refreshToken, response);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal User user,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ErrorType.UNAUTHORIZED_ACCESS));
        }

        String refreshToken = CookieUtils.extractRefreshToken(request);
        authService.logout(user, refreshToken, request, response);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/logout/all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @AuthenticationPrincipal User user,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ErrorType.UNAUTHORIZED_ACCESS));
        }

        authService.logoutAll(user, request, response);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
