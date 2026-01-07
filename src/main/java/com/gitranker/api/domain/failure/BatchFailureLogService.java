package com.gitranker.api.domain.failure;

import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
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

        MdcUtils.setLogContext(LogCategory.SYSTEM, EventType.REQUEST);

        try {
            BatchFailureLog failureLog = BatchFailureLog.builder()
                    .jobName(jobName)
                    .targetId(targetId)
                    .errorType(errorType)
                    .errorMessage(errorMessage)
                    .build();

            batchFailureLogRepository.save(failureLog);

            MdcUtils.setEventType(EventType.SUCCESS);
            log.info("배치 실패 로그 저장 완료 - Job: {}, Target: {}, ErrorType: {}",
                    jobName, targetId, errorType.name());

        } catch (Exception e) {
            MdcUtils.setLogContext(LogCategory.SYSTEM, EventType.FAILURE);
            MdcUtils.setError(e.getClass().getSimpleName(), e.getMessage());

            log.error("배치 실패 로그 저장 중 오류 - Job: {}, Target: {}", jobName, targetId, e);
        }
    }
}
