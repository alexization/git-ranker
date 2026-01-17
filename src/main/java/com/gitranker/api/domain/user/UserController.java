package com.gitranker.api.domain.user;

import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.domain.user.service.UserQueryService;
import com.gitranker.api.domain.user.service.UserRefreshService;
import com.gitranker.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserQueryService userQueryService;
    private final UserRefreshService userRefreshService;

    @GetMapping("/{username}")
    public ApiResponse<RegisterUserResponse> getUser(@PathVariable String username) {
        RegisterUserResponse response = userQueryService.findByUsername(username);

        return ApiResponse.success(response);
    }

    @PostMapping("/{username}/refresh")
    public ApiResponse<RegisterUserResponse> refreshUser(@PathVariable String username) {
        RegisterUserResponse response = userRefreshService.refresh(username);

        return ApiResponse.success(response);
    }
}


