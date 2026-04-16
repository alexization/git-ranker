package com.gitranker.api.batch.reader;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserItemReaderTest {

    @InjectMocks
    private UserItemReader userItemReader;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("reader is configured with repository pagination and ascending id sort")
    void createsConfiguredRepositoryReader() {
        RepositoryItemReader<User> reader = userItemReader.createReader(37);

        assertThat(ReflectionTestUtils.getField(reader, "repository")).isSameAs(userRepository);
        assertThat(ReflectionTestUtils.getField(reader, "methodName")).isEqualTo("findAll");
        assertThat(ReflectionTestUtils.getField(reader, "pageSize")).isEqualTo(37);
        assertThat(ReflectionTestUtils.getField(reader, "sort")).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
    }
}
