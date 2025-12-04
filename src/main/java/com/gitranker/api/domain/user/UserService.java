package com.gitranker.api.domain.user;

import com.gitranker.api.domain.ranking.RankingInfo;
import com.gitranker.api.domain.ranking.RankingService;
import com.gitranker.api.domain.user.dto.RegisterUserResponse;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.GitHubApiClient;
import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import com.gitranker.api.infrastructure.github.dto.GitHubUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final GitHubApiClient gitHubApiClient;
    private final GitHubActivityService gitHubActivityService;
    private final RankingService rankingService;

    @Transactional
    public RegisterUserResponse registerUser(String username) {
        GitHubUserResponse githubUser = gitHubApiClient.getUser(username);

        return userRepository.findByNodeId(githubUser.nodeId())
                .map(existingUser -> handleExistingUser(existingUser, githubUser))
                .orElseGet(() -> handleNewUser(githubUser));
    }

    private RegisterUserResponse handleExistingUser(User existingUser, GitHubUserResponse githubUser) {
        log.info("기존 사용자 정보 업데이트 : {}", githubUser.login());

        existingUser.updateUsername(githubUser.login());
        existingUser.updateProfileImage(githubUser.avatarUrl());

        return RegisterUserResponse.from(existingUser, false);
    }

    private RegisterUserResponse handleNewUser(GitHubUserResponse githubUser) {
        log.info("신규 사용자 등록 시작 : {}", githubUser.login());

        User newUser = User.builder()
                .nodeId(githubUser.nodeId())
                .username(githubUser.login())
                .profileImage(githubUser.avatarUrl())
                .build();

        userRepository.save(newUser);
        log.info("사용자 기본 정보 저장 완료 : ID = {}", newUser.getId());

        GitHubActivitySummary summary = gitHubActivityService.collectAllActivities(githubUser.login());

        int totalScore = summary.calculateTotalScore();
        newUser.addScore(totalScore);

        RankingInfo rankingInfo = rankingService.calculateRankingForNewUser(totalScore);
        newUser.updateRankInfo(
                rankingInfo.ranking(),
                rankingInfo.percentile(),
                rankingInfo.tier()
        );

        log.info("신규 사용자 등록 완료 : {}, 총점 : {}, {}", githubUser.login(), totalScore, rankingInfo);

        return RegisterUserResponse.from(newUser, true);
    }
}
