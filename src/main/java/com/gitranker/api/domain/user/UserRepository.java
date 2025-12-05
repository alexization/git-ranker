package com.gitranker.api.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNodeId(String nodeId);

    long countByTotalScoreGreaterThan(int totalScoreIsGreaterThan);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE users u
                JOIN(
                    SELECT
                        id,
                            RANK() OVER (ORDER BY total_score DESC) as new_rank,
                                CUME_DIST() OVER (ORDER BY total_score DESC) as new_percentile
                                    FROM users
                    ) r ON u.id = r.id
                        SET
                            u.ranking = r.new_rank,
                                u.percentil = r.new_percentile * 100,
                                    u.tier = CASE
                                        WHEN r.new_percentile <= 0.01 THEN 'DIAMOND'
                                        WHEN r.new_percentile <= 0.05 THEN 'PLATINUM'
                                        WHEN r.new_percentile <= 0.10 THEN 'GOLD'
                                        WHEN r.new_percentile <= 0.25 THEN 'SILVER'
                                        WHEN r.new_percentile <= 0.50 THEN 'BRONZE'
                                        ELSE 'IRON'
                                            END,
                                                u.updated_at = NOW()
            """, nativeQuery = true)
    void bulkUpdateRanking();
}
