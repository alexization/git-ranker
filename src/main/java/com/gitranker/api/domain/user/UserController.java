package com.gitranker.api.domain.user;

import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.domain.user.service.UserDeletionService;
import com.gitranker.api.domain.user.service.UserQueryService;
import com.gitranker.api.domain.user.service.UserRefreshService;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@Tag(name = "Users")
@RequestMapping("/api/v1/users")
public class UserController {

    private static final String USERNAME_PATTERN = "^(?=.{1,39}$)[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$";
    private static final String USERNAME_MESSAGE = "{validation.user.username.pattern}";
    private final UserQueryService userQueryService;
    private final UserRefreshService userRefreshService;
    private final UserDeletionService userDeletionService;

    @GetMapping("/{username}")
    @Operation(summary = "Get a user's profile", description = "Returns the public Git Ranker profile for a GitHub username.")
    public ApiResponse<RegisterUserResponse> getUser(
            @PathVariable @Pattern(regexp = USERNAME_PATTERN, message = USERNAME_MESSAGE) String username
    ) {
        RegisterUserResponse response = userQueryService.findByUsername(username);

        return ApiResponse.success(response);
    }

    @PostMapping("/{username}/refresh")
    @Operation(
            summary = "Refresh the authenticated user's score",
            description = "Recalculates the caller's own profile. The authenticated user must match the path username.",
            security = {
                    @SecurityRequirement(name = "bearerAuth"),
                    @SecurityRequirement(name = "accessTokenCookie")
            }
    )
    public ApiResponse<RegisterUserResponse> refreshUser(
            @PathVariable @Pattern(regexp = USERNAME_PATTERN, message = USERNAME_MESSAGE) String username,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            throw new BusinessException(ErrorType.UNAUTHORIZED);
        }

        if (!user.getUsername().equals(username)) {
            throw new BusinessException(ErrorType.FORBIDDEN);
        }

        RegisterUserResponse response = userRefreshService.refresh(username);

        return ApiResponse.success(response);
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "Delete the authenticated user's account",
            description = "Deletes the current account and clears authentication cookies.",
            security = {
                    @SecurityRequirement(name = "bearerAuth"),
                    @SecurityRequirement(name = "accessTokenCookie")
            }
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> deleteMyAccount(
            @AuthenticationPrincipal User user,
            HttpServletResponse response
    ) {
        if (user == null) {
            throw new BusinessException(ErrorType.UNAUTHORIZED);
        }

        userDeletionService.deleteAccount(user, response);

        return ResponseEntity.noContent().build();
    }
}
