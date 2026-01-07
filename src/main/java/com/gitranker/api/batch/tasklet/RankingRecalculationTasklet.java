package com.gitranker.api.batch.tasklet;

import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
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
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        MdcUtils.setLogContext(LogCategory.BATCH, EventType.REQUEST);
        MdcUtils.setStepName("RankingRecalculationStep");

        log.debug("랭킹 벌크 업데이트 시작");

        try {
            long start = System.currentTimeMillis();

            userRepository.bulkUpdateRanking();

            long latency = System.currentTimeMillis() - start;

            MdcUtils.setEventType(EventType.SUCCESS);
            log.info("랭킹 벌크 업데이트 완료 - Latency: {}ms", latency);

        } catch (Exception e) {
            MdcUtils.setLogContext(LogCategory.BATCH, EventType.FAILURE);
            MdcUtils.setError(e.getClass().getSimpleName(), e.getMessage());

            log.error("랭킹 벌크 업데이트 실패 - Reason: {}", e.getMessage(), e);

            throw new BusinessException(ErrorType.BATCH_STEP_FAILED, "랭킹 재산정 실패");
        }

        return RepeatStatus.FINISHED;
    }
}
