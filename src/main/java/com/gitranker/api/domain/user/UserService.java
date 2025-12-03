package com.gitranker.api.domain.user;

import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.infrastructure.github.GitHubApiClient;
import com.gitranker.api.infrastructure.github.dto.GitHubUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final GitHubApiClient gitHubApiClient;

    @Transactional
    public RegisterUserResponse registerUser(String username) {
        GitHubUserResponse githubUser = gitHubApiClient.getUser(username);

        return userRepository.findByNodeId(githubUser.nodeId())
                .map(existingUser -> {
                    existingUser.updateUsername(githubUser.login());
                    existingUser.updateProfileImage(githubUser.avatarUrl());

                    return RegisterUserResponse.from(existingUser, false);
                }).orElseGet(() -> {
                    User newUser = User.builder()
                            .nodeId(githubUser.nodeId())
                            .username(githubUser.login())
                            .profileImage(githubUser.avatarUrl())
                            .build();

                    userRepository.save(newUser);
                    return RegisterUserResponse.from(newUser, true);
                });
    }
}
