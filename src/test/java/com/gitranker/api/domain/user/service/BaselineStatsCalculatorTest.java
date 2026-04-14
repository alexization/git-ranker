package com.gitranker.api.domain.user.service;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import com.gitranker.api.infrastructure.github.GitHubDataMapper;
import com.gitranker.api.infrastructure.github.dto.GitHubAllActivitiesResponse;
import com.gitranker.api.domain.user.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.gitranker.api.support.TestFixtures.stats;
import static com.gitranker.api.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaselineStatsCalculatorTest {

    @Mock
    private GitHubDataMapper gitHubDataMapper;

    @Test
    @DisplayName("가입 연도가 현재보다 이전이면 작년까지의 baseline 통계를 계산한다")
    void calculatesBaselineForExistingUsers() {
        LocalDate fixedDate = LocalDate.of(2026, 1, 1);
        BaselineStatsCalculator calculator = createCalculatorWithFixedDate(fixedDate);
        User user = user("existing-user");
        GitHubAllActivitiesResponse rawResponse = GitHubAllActivitiesResponse.empty();
        ActivityStatistics expected = stats(10, 2, 3, 1, 4);
        int expectedLastYear = fixedDate.getYear() - 1;

        when(gitHubDataMapper.calculateStatisticsUntilYear(rawResponse, expectedLastYear)).thenReturn(expected);

        ActivityStatistics actual = calculator.calculate(user, rawResponse);

        assertThat(actual).isEqualTo(expected);
        verify(gitHubDataMapper).calculateStatisticsUntilYear(rawResponse, expectedLastYear);
    }

    @Test
    @DisplayName("가입 연도가 현재 연도면 baseline 통계를 계산하지 않는다")
    void returnsNullForUsersCreatedThisYear() {
        LocalDate fixedDate = LocalDate.of(2026, 1, 1);
        BaselineStatsCalculator calculator = createCalculatorWithFixedDate(fixedDate);
        User user = new User(
                2L,
                "node-current",
                "current-user",
                "current@example.com",
                "https://images.example.com/current-user.png",
                LocalDateTime.of(fixedDate.getYear(), 1, 1, 0, 0),
                Role.USER
        );

        ActivityStatistics actual = calculator.calculate(user, GitHubAllActivitiesResponse.empty());

        assertThat(actual).isNull();
        verifyNoInteractions(gitHubDataMapper);
    }

    private BaselineStatsCalculator createCalculatorWithFixedDate(LocalDate fixedDate) {
        BaselineStatsCalculator calculator = Mockito.spy(new BaselineStatsCalculator(gitHubDataMapper));
        Mockito.doReturn(fixedDate).when(calculator).currentDate();
        return calculator;
    }
}
