package com.gitranker.api.domain.user;

import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.domain.user.service.UserDeletionService;
import com.gitranker.api.domain.user.service.UserQueryService;
import com.gitranker.api.domain.user.service.UserRefreshService;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserQueryService userQueryService;
    private final UserRefreshService userRefreshService;
    private final UserDeletionService userDeletionService;

    @GetMapping("/{username}")
    public ApiResponse<RegisterUserResponse> getUser(@PathVariable String username) {
        RegisterUserResponse response = userQueryService.findByUsername(username);

        return ApiResponse.success(response);
    }

    @PostMapping("/{username}/refresh")
    public ApiResponse<RegisterUserResponse> refreshUser(
            @PathVariable String username,
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


