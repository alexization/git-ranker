package com.gitranker.api.domain.log;

import com.gitranker.api.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    ActivityLog getTopByUserOrderByActivityDateDesc(User user);

    Optional<ActivityLog> findTopByUserAndActivityDateLessThanOrderByActivityDateDesc(User user, LocalDate date);

    Optional<ActivityLog> findByUserAndActivityDate(User user, LocalDate activityDate);

    void deleteAllByUser(User user);
}
