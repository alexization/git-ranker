package com.gitranker.api.batch.writer;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
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
        userRepository.saveAll(chunk.getItems());

        log.info("사용자 {} 명 업데이트 완료", chunk.size());
    }
}
