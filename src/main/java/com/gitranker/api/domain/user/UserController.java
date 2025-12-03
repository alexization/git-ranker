package com.gitranker.api.domain.user;

import com.gitranker.api.domain.user.dto.RegisterUserRequest;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<RegisterUserResponse>> registerUser(@RequestBody RegisterUserRequest request) {
        RegisterUserResponse response = userService.registerUser(request.username());

        return ResponseEntity
                .status(response.isNewUser() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.success(response));
    }
}


