package com.gitranker.api.domain.user.dto;

import com.gitranker.api.domain.user.User;

public record RegisterUserResponse(Long userId, String username, String profileImage, boolean isNewUser) {
    public static RegisterUserResponse from(User user, boolean isNewUser) {
        return new RegisterUserResponse(user.getId(), user.getUsername(), user.getProfileImage(), isNewUser);
    }
}
