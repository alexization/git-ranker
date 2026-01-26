package com.gitranker.api.domain.failure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchFailureLogRepository extends JpaRepository<BatchFailureLog, Long> {

    List<BatchFailureLog> findByJobNameOrderByCreatedAtDesc(String jobName);

    List<BatchFailureLog> findByTargetId(String targetId);

    void deleteAllByTargetId(String targetId);
}
