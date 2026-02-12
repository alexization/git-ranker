package com.gitranker.api.domain.log;

import com.gitranker.api.domain.user.Role;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.domain.user.vo.ActivityStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.batch.jdbc.initialize-schema=never"
})
class ActivityLogRepositoryIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("gitranker_test");

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        activityLogRepository.deleteAll();
        userRepository.deleteAll();

        savedUser = userRepository.save(User.builder()
                .githubId(1L)
                .nodeId("node1")
                .username("testuser")
                .githubCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .role(Role.USER)
                .build());
    }

    @Test
    @DisplayName("여러 로그 중 가장 최근 날짜의 로그를 조회한다")
    void should_findLatestLog_when_multipleLogsExist() {
        ActivityStatistics stats = ActivityStatistics.of(10, 2, 1, 0, 3);
        ActivityStatistics diff = ActivityStatistics.of(5, 1, 0, 0, 1);

        activityLogRepository.save(ActivityLog.of(savedUser, stats, diff, LocalDate.of(2025, 1, 1)));
        activityLogRepository.save(ActivityLog.of(savedUser, stats, diff, LocalDate.of(2025, 1, 2)));
        ActivityLog latestLog = activityLogRepository.save(
                ActivityLog.of(savedUser, stats, diff, LocalDate.of(2025, 1, 3)));

        ActivityLog found = activityLogRepository.getTopByUserOrderByActivityDateDesc(savedUser);

        assertThat(found).isNotNull();
        assertThat(found.getActivityDate()).isEqualTo(LocalDate.of(2025, 1, 3));
    }

    @Test
    @DisplayName("로그가 없으면 null을 반환한다")
    void should_returnNull_when_noLogExists() {
        ActivityLog found = activityLogRepository.getTopByUserOrderByActivityDateDesc(savedUser);

        assertThat(found).isNull();
    }

    @Test
    @DisplayName("특정 날짜 이전의 가장 최근 로그를 조회한다")
    void should_findBaselineLog_when_logBeforeDateExists() {
        ActivityStatistics stats = ActivityStatistics.of(10, 2, 1, 0, 3);
        ActivityStatistics diff = ActivityStatistics.of(5, 1, 0, 0, 1);

        activityLogRepository.save(ActivityLog.of(savedUser, stats, diff, LocalDate.of(2025, 1, 1)));
        activityLogRepository.save(ActivityLog.of(savedUser, stats, diff, LocalDate.of(2025, 1, 10)));
        activityLogRepository.save(ActivityLog.of(savedUser, stats, diff, LocalDate.of(2025, 1, 20)));

        // 1월 15일 이전의 가장 최근 로그 → 1월 10일
        Optional<ActivityLog> found = activityLogRepository
                .findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(
                        savedUser, LocalDate.of(2025, 1, 15));

        assertThat(found).isPresent();
        assertThat(found.get().getActivityDate()).isEqualTo(LocalDate.of(2025, 1, 10));
    }

    @Test
    @DisplayName("기준 날짜 이전에 로그가 없으면 빈 Optional을 반환한다")
    void should_returnEmpty_when_noLogBeforeDateExists() {
        ActivityStatistics stats = ActivityStatistics.of(10, 2, 1, 0, 3);
        ActivityStatistics diff = ActivityStatistics.of(5, 1, 0, 0, 1);

        activityLogRepository.save(ActivityLog.of(savedUser, stats, diff, LocalDate.of(2025, 6, 1)));

        // 2025년 1월 이전 → 없음
        Optional<ActivityLog> found = activityLogRepository
                .findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(
                        savedUser, LocalDate.of(2025, 1, 1));

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("사용자와 날짜로 정확히 일치하는 로그를 조회한다")
    void should_findLog_when_userAndDateMatch() {
        ActivityStatistics stats = ActivityStatistics.of(50, 10, 5, 3, 8);
        ActivityStatistics diff = ActivityStatistics.of(5, 1, 1, 0, 2);

        activityLogRepository.save(ActivityLog.of(savedUser, stats, diff, LocalDate.of(2025, 3, 15)));

        Optional<ActivityLog> found = activityLogRepository.findByUserAndActivityDate(
                savedUser, LocalDate.of(2025, 3, 15));

        assertThat(found).isPresent();
        assertThat(found.get().getCommitCount()).isEqualTo(50);
        assertThat(found.get().getIssueCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("사용자의 모든 로그를 삭제한다")
    void should_deleteAllLogs_when_deleteAllByUserCalled() {
        ActivityStatistics stats = ActivityStatistics.of(10, 2, 1, 0, 3);
        ActivityStatistics diff = ActivityStatistics.of(5, 1, 0, 0, 1);

        activityLogRepository.save(ActivityLog.of(savedUser, stats, diff, LocalDate.of(2025, 1, 1)));
        activityLogRepository.save(ActivityLog.of(savedUser, stats, diff, LocalDate.of(2025, 1, 2)));

        activityLogRepository.deleteAllByUser(savedUser);

        assertThat(activityLogRepository.getTopByUserOrderByActivityDateDesc(savedUser)).isNull();
    }
}
