package com.gitranker.api.domain.log;

import com.gitranker.api.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    ActivityLog getTopByUserOrderByActivityDateDesc(User user);
}
