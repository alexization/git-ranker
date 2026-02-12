package com.gitranker.api.domain.user;

import com.gitranker.api.domain.user.vo.Score;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.batch.jdbc.initialize-schema=never"
})
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("gitranker_test");

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private User createAndSaveUser(String username, String nodeId, Long githubId, int score) {
        User user = User.builder()
                .githubId(githubId)
                .nodeId(nodeId)
                .username(username)
                .githubCreatedAt(LocalDateTime.of(2020, 1, 1, 0, 0))
                .role(Role.USER)
                .build();
        user.updateScore(Score.of(score));
        return userRepository.save(user);
    }

    @Test
    @DisplayName("bulkUpdateRanking 호출 시 점수 기준으로 ranking, percentile, tier가 올바르게 계산된다")
    void should_updateAllRankings_when_bulkUpdateRankingCalled() {
        // 3명의 사용자: 점수 3000, 1500, 100
        createAndSaveUser("high", "node-high", 1L, 3000);
        createAndSaveUser("mid", "node-mid", 2L, 1500);
        createAndSaveUser("low", "node-low", 3L, 100);

        userRepository.bulkUpdateRanking();

        // bulkUpdateRanking은 @Modifying(clearAutomatically = true)이므로 영속성 컨텍스트가 초기화됨
        User high = userRepository.findByUsername("high").orElseThrow();
        User mid = userRepository.findByUsername("mid").orElseThrow();
        User low = userRepository.findByUsername("low").orElseThrow();

        // RANK() OVER (ORDER BY total_score DESC)
        assertThat(high.getRanking()).isEqualTo(1);
        assertThat(mid.getRanking()).isEqualTo(2);
        assertThat(low.getRanking()).isEqualTo(3);

        // CUME_DIST() OVER (ORDER BY total_score DESC) * 100
        // high: 1/3 * 100 ≈ 33.33, mid: 2/3 * 100 ≈ 66.67, low: 3/3 * 100 = 100.0
        assertThat(high.getPercentile()).isCloseTo(33.33, org.assertj.core.data.Offset.offset(0.1));
        assertThat(mid.getPercentile()).isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.1));
        assertThat(low.getPercentile()).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.1));

        // 티어 검증: SQL의 CASE 분기와 Java RankInfo.calculateTier()가 동일한 결과
        // high: score=3000 >= 2000, percentile=33.33 <= 45 → PLATINUM
        // mid: score=1500 >= 1500 → GOLD
        // low: score=100 < 500 → IRON
        assertThat(high.getTier()).isEqualTo(Tier.PLATINUM);
        assertThat(mid.getTier()).isEqualTo(Tier.GOLD);
        assertThat(low.getTier()).isEqualTo(Tier.IRON);
    }

    @Test
    @DisplayName("동점자가 있을 때 같은 ranking이 부여된다")
    void should_assignSameRanking_when_scoresAreTied() {
        createAndSaveUser("user1", "node1", 1L, 1000);
        createAndSaveUser("user2", "node2", 2L, 1000);
        createAndSaveUser("user3", "node3", 3L, 500);

        userRepository.bulkUpdateRanking();

        User user1 = userRepository.findByUsername("user1").orElseThrow();
        User user2 = userRepository.findByUsername("user2").orElseThrow();
        User user3 = userRepository.findByUsername("user3").orElseThrow();

        // RANK()는 동점자에게 같은 순위 부여
        assertThat(user1.getRanking()).isEqualTo(1);
        assertThat(user2.getRanking()).isEqualTo(1);
        assertThat(user3.getRanking()).isEqualTo(3); // 1등이 2명이므로 3등

        // 동점자 티어: score=1000 → SILVER
        assertThat(user1.getTier()).isEqualTo(Tier.SILVER);
        assertThat(user2.getTier()).isEqualTo(Tier.SILVER);
        assertThat(user3.getTier()).isEqualTo(Tier.BRONZE);
    }

    @Test
    @DisplayName("nodeId로 사용자를 조회할 수 있다")
    void should_findUser_when_nodeIdExists() {
        createAndSaveUser("testuser", "unique-node-id", 1L, 100);

        Optional<User> found = userRepository.findByNodeId("unique-node-id");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("존재하지 않는 nodeId로 조회하면 빈 Optional을 반환한다")
    void should_returnEmpty_when_nodeIdDoesNotExist() {
        Optional<User> found = userRepository.findByNodeId("non-existent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("username으로 사용자를 조회할 수 있다")
    void should_findUser_when_usernameExists() {
        createAndSaveUser("findme", "node-find", 1L, 100);

        Optional<User> found = userRepository.findByUsername("findme");

        assertThat(found).isPresent();
        assertThat(found.get().getNodeId()).isEqualTo("node-find");
    }

    @Test
    @DisplayName("특정 점수보다 높은 사용자 수를 정확히 카운트한다")
    void should_countCorrectly_when_countByScoreValueGreaterThan() {
        createAndSaveUser("user1", "node1", 1L, 3000);
        createAndSaveUser("user2", "node2", 2L, 1500);
        createAndSaveUser("user3", "node3", 3L, 100);

        long count = userRepository.countByScoreValueGreaterThan(1000);

        // score > 1000: user1(3000), user2(1500) → 2명
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("점수 내림차순으로 페이징 조회가 동작한다")
    void should_returnPagedResults_when_findAllByScoreDesc() {
        createAndSaveUser("user1", "node1", 1L, 3000);
        createAndSaveUser("user2", "node2", 2L, 1500);
        createAndSaveUser("user3", "node3", 3L, 100);

        Page<User> firstPage = userRepository.findAllByOrderByScoreValueDesc(PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getContent().get(0).getUsername()).isEqualTo("user1");
        assertThat(firstPage.getContent().get(1).getUsername()).isEqualTo("user2");
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("티어별로 필터링하여 조회할 수 있다")
    void should_filterByTier_when_tierProvided() {
        createAndSaveUser("user1", "node1", 1L, 3000);
        createAndSaveUser("user2", "node2", 2L, 1500);
        createAndSaveUser("user3", "node3", 3L, 100);

        // bulkUpdateRanking으로 티어를 실제 계산한 후 필터링 테스트
        userRepository.bulkUpdateRanking();

        Page<User> goldUsers = userRepository.findAllByRankInfoTierOrderByScoreValueDesc(
                Tier.GOLD, PageRequest.of(0, 10));

        assertThat(goldUsers.getContent()).hasSize(1);
        assertThat(goldUsers.getContent().get(0).getUsername()).isEqualTo("user2");
    }
}
