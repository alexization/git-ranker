package com.gitranker.api.domain.failure;

import com.gitranker.api.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchFailureLogService {
    private final BatchFailureLogRepository batchFailureLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailureLog(String jobName, String targetId, ErrorType errorType, String errorMessage) {
        try {
            BatchFailureLog failureLog = BatchFailureLog.builder()
                    .jobName(jobName)
                    .targetId(targetId)
                    .errorType(errorType)
                    .errorMessage(errorMessage)
                    .build();

            batchFailureLogRepository.save(failureLog);
        } catch (Exception e) {
            log.error("[Batch Error] 실패 로그 저장 중 오류 발생 - Target: {}", targetId, e);
        }
    }
}
