package com.gitranker.api.batch.writer;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.global.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserItemWriter implements ItemWriter<User> {

    private final UserRepository userRepository;

    @Override
    public void write(Chunk<? extends User> chunk) throws Exception {
        try {
            userRepository.saveAll(chunk.getItems());

            log.debug("배치 Chunk 저장 완료 - Size: {}", chunk.getItems().size());

        } catch (Exception e) {
            log.error("배치 Chunk 저장 실패 - Size: {}, Reason: {}", chunk.getItems().size(), e.getMessage(), e);

            throw new BusinessException(ErrorType.BATCH_STEP_FAILED, "DB 저장 실패");
        }
    }
}
