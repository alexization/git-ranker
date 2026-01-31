package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.infrastructure.github.GitHubDataMapper;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Baseline 통계 계산을 담당하는 컴포넌트.
 * 사용자의 GitHub 가입 연도를 기준으로 작년까지의 누적 활동 통계를 계산합니다.
 */
@Component
@RequiredArgsConstructor
public class BaselineStatsCalculator {

    private final GitHubDataMapper gitHubDataMapper;

    public ActivityStatistics calculate(User user, GitHubAllActivitiesResponse rawResponse) {
        int currentYear = LocalDate.now().getYear();
        int userJoinYear = user.getGithubCreatedAt().getYear();

        if (userJoinYear < currentYear) {
            int lastYear = currentYear - 1;
            return gitHubDataMapper.calculateStatisticsUntilYear(rawResponse, lastYear);
        }

        return null;
    }
}
