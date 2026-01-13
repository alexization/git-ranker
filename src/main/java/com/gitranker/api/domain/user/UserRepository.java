package com.gitranker.api.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNodeId(String nodeId);

    Optional<User> findByUsername(String username);

    long countByScoreValueGreaterThan(int value);

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
                                            u.percentile = r.new_percentile * 100,
                                                u.tier = CASE
                                                    WHEN r.new_percentile <= 0.01 AND u.total_score >= 2000 THEN 'CHALLENGER'
                                                            WHEN r.new_percentile <= 0.05 AND u.total_score >= 2000 THEN 'MASTER'
                                                            WHEN r.new_percentile <= 0.12 AND u.total_score >= 2000 THEN 'DIAMOND'
                                                            WHEN r.new_percentile <= 0.25 AND u.total_score >= 2000 THEN 'EMERALD'
                                                            WHEN r.new_percentile <= 0.45 AND u.total_score >= 2000 THEN 'PLATINUM'
                                                            WHEN u.total_score >= 1500 THEN 'GOLD'
                                                            WHEN u.total_score >= 1000 THEN 'SILVER'
                                                            WHEN u.total_score >= 500 THEN 'BRONZE'
                                                            ELSE 'IRON'
                                                        END,
                                                            u.updated_at = NOW()
            """, nativeQuery = true)
    void bulkUpdateRanking();

    Page<User> findAllByOrderByScoreValueDesc(Pageable pageable);

    Page<User> findAllByRankInfoTierOrderByScoreValueDesc(Tier tier, Pageable pageable);
}
