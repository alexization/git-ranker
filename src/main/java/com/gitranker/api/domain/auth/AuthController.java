package com.gitranker.api.domain.auth;

import com.gitranker.api.domain.auth.dto.AuthMeResponse;
import com.gitranker.api.domain.auth.service.AuthService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.response.ApiResponse;
import com.gitranker.api.global.util.CookieUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth")
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/me")
    @Operation(
            summary = "Get the current authenticated user",
            description = "Returns the current session user resolved from the access token.",
            security = {
                    @SecurityRequirement(name = "bearerAuth"),
                    @SecurityRequirement(name = "accessTokenCookie")
            }
    )
    public ResponseEntity<ApiResponse<AuthMeResponse>> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ErrorType.UNAUTHORIZED_ACCESS));
        }

        return ResponseEntity.ok(ApiResponse.success(AuthMeResponse.from(user)));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access and refresh tokens",
            description = "Requires the refreshToken cookie and rotates the active session tokens.",
            security = @SecurityRequirement(name = "refreshTokenCookie")
    )
    public ResponseEntity<ApiResponse<Void>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = CookieUtils.extractRefreshToken(request);
        authService.refreshAccessToken(refreshToken, response);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Log out the current session",
            description = "Requires an authenticated session and the refreshToken cookie to invalidate the current login.",
            security = {
                    @SecurityRequirement(name = "bearerAuth"),
                    @SecurityRequirement(name = "accessTokenCookie")
            }
    )
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
    @Operation(
            summary = "Log out every session for the current user",
            description = "Revokes all refresh tokens for the authenticated user.",
            security = {
                    @SecurityRequirement(name = "bearerAuth"),
                    @SecurityRequirement(name = "accessTokenCookie")
            }
    )
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
