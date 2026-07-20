package com.gitranker.api.batch.dto;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;

import java.time.LocalDate;

/**
 * 프로세서가 계산한 점수/활동 통계를 writer로 넘기는 carrier.
 * DB 쓰기는 writer가 청크 트랜잭션 안에서 수행하고, 프로세서는 순수 계산 결과만 담는다.
 */
public record ScoredUserUpdate(
        User user,
        ActivityStatistics stats,
        ActivityStatistics diff,
        LocalDate date
) {
}
