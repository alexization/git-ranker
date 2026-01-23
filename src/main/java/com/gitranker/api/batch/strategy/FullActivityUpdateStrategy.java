package com.gitranker.api.batch.strategy;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FullActivityUpdateStrategy implements ActivityUpdateStrategy {

    private final GitHubActivityService activityService;

    @Override
    public ActivityStatistics update(User user, ActivityUpdateContext context) {
        GitHubAllActivitiesResponse fullResponse =
                activityService.fetchRawAllActivities(user.getUsername(), user.getGithubCreatedAt());

        ActivityStatistics stats = activityService.convertToSummary(fullResponse).toActivityStatistics();

        log.info("전체 업데이트 완료 - 사용자: {}", user.getUsername());

        return stats;
    }
}
