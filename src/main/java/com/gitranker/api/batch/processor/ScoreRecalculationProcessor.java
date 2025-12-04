package com.gitranker.api.batch.processor;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.dto.GitHubActivitySummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreRecalculationProcessor implements ItemProcessor<User, User> {
    private final GitHubActivityService activityService;

    @Override
    public User process(User user) {
        try {
            GitHubActivitySummary summary = activityService.collectAllActivities(user.getUsername());

            int newScore = summary.calculateTotalScore();
            user.updateScore(newScore);

            return user;
        } catch (Exception e) {
            log.error("점수 재계산 실패 - 사용자 : {}, 에러 : {}", user.getUsername(), e.getMessage());

            return null;
        }
    }
}
