package com.gitranker.api.domain.user.dto;

import com.gitranker.api.domain.user.User;

public record RegisterUserRes(Long userId, String username, String profileImage, boolean isNewUser) {
    public static RegisterUserRes from(User user, boolean isNewUser) {
        return new RegisterUserRes(user.getId(), user.getUsername(), user.getProfileImage(), isNewUser);
    }
}
