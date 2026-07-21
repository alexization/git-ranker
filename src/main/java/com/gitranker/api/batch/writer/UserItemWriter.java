package com.gitranker.api.batch.writer;

import com.gitranker.api.batch.dto.ScoredUserUpdate;
import com.gitranker.api.domain.log.ActivityLogService;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserItemWriter implements ItemWriter<ScoredUserUpdate> {

    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Override
    public void write(Chunk<? extends ScoredUserUpdate> chunk) throws Exception {
        try {
            List<User> users = chunk.getItems().stream()
                    .map(ScoredUserUpdate::user)
                    .toList();
            userRepository.saveAll(users);

            for (ScoredUserUpdate update : chunk.getItems()) {
                upsertActivityLog(update);
            }

            log.debug("배치 Chunk 저장 완료 - Size: {}", users.size());

        } catch (Exception e) {
            throw new BusinessException(ErrorType.BATCH_STEP_FAILED, "DB 저장 실패");
        }
    }

    private void upsertActivityLog(ScoredUserUpdate update) {
        activityLogService.findByDate(update.user(), update.date())
                .ifPresentOrElse(
                        existingLog -> activityLogService.updateActivityLog(existingLog, update.stats(), update.diff()),
                        () -> activityLogService.saveActivityLog(update.user(), update.stats(), update.diff(), update.date())
                );
    }
}
