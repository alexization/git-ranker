package com.gitranker.api.batch.strategy;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.global.logging.LogSanitizer;
import com.gitranker.api.infrastructure.github.GitHubActivityService;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FullActivityUpdateStrategy implements ActivityUpdateStrategy {

    private final GitHubActivityService activityService;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ActivityStatistics update(User user, ActivityUpdateContext context) {
        GitHubAllActivitiesResponse fullResponse =
                activityService.fetchRawAllActivities(user.getUsername(), user.getGithubCreatedAt());

        ActivityStatistics stats = activityService.toSummary(fullResponse).toActivityStatistics();

        log.debug("전체 업데이트 완료 - 사용자: {}", LogSanitizer.maskUsername(user.getUsername()));

        return stats;
    }
}
