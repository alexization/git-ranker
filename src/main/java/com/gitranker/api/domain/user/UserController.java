package com.gitranker.api.domain.user;

import com.gitranker.api.domain.user.dto.RegisterUserReq;
import com.gitranker.api.domain.user.dto.RegisterUserRes;
import com.gitranker.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    public ApiResponse<RegisterUserRes> registerUser(@RequestBody RegisterUserReq request) {
        RegisterUserRes response = userService.registerUser(request.username());

        return ApiResponse.success(response);
    }
}


