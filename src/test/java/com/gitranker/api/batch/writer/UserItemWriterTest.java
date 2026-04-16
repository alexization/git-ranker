package com.gitranker.api.batch.writer;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import com.gitranker.api.global.error.ErrorType;
import com.gitranker.api.global.error.exception.BusinessException;
import com.gitranker.api.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserItemWriterTest {

    @InjectMocks
    private UserItemWriter userItemWriter;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("writer saves all users in the chunk")
    void savesChunkUsers() throws Exception {
        User first = TestFixtures.user("alice");
        User second = TestFixtures.user("bob");
        Chunk<User> chunk = new Chunk<>(List.of(first, second));

        userItemWriter.write(chunk);

        verify(userRepository).saveAll(chunk.getItems());
    }

    @Test
    @DisplayName("writer wraps repository failures with batch-step exception")
    void wrapsRepositoryFailure() {
        Chunk<User> chunk = new Chunk<>(List.of(TestFixtures.user("alice")));

        when(userRepository.saveAll(chunk.getItems())).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> userItemWriter.write(chunk))
                .isInstanceOf(BusinessException.class)
                .satisfies(throwable -> {
                    BusinessException exception = (BusinessException) throwable;
                    assertThat(exception.getErrorType()).isEqualTo(ErrorType.BATCH_STEP_FAILED);
                    assertThat(exception.getData()).isEqualTo("DB 저장 실패");
                });
    }
}
