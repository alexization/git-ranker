package com.gitranker.api.domain.auth.dto;

import com.gitranker.api.domain.user.Role;
import com.gitranker.api.domain.user.User;

public record AuthMeResponse(
        String username,
        String profileImage,
        Role role
) {
    public static AuthMeResponse from(User user) {
        return new AuthMeResponse(
                user.getUsername(),
                user.getProfileImage(),
                user.getRole()
        );
    }
}
