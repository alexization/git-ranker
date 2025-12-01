package com.gitranker.api.domain.user;

import com.gitranker.api.domain.user.dto.RegisterUserRes;
import com.gitranker.api.infrastructure.github.GitHubApiClient;
import com.gitranker.api.infrastructure.github.GitHubUserResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final GitHubApiClient gitHubApiClient;

    @Transactional
    public RegisterUserRes registerUser(String username) {
        GitHubUserResponse githubUser = gitHubApiClient.getUser(username);

        return userRepository.findByNodeId(githubUser.nodeId()).map(existingUser -> {
            existingUser.updateUsername(githubUser.login());
            existingUser.updateProfileImage(githubUser.avatarUrl());
            return RegisterUserRes.from(existingUser, false);
        }).orElseGet(() -> {
            User newUser = User.builder().nodeId(githubUser.nodeId()).username(githubUser.login()).profileImage(githubUser.avatarUrl()).build();

            userRepository.save(newUser);
            return RegisterUserRes.from(newUser, true);
        });
    }
}
