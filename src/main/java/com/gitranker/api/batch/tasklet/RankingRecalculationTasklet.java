package com.gitranker.api.batch.tasklet;

import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.aop.LogExecutionTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingRecalculationTasklet implements Tasklet {

    private final UserRepository userRepository;

    @Override
    @LogExecutionTime
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("[Batch Step] 순위 일괄 재계산 시작");

        userRepository.bulkUpdateRanking();

        return RepeatStatus.FINISHED;
    }
}
