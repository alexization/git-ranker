package com.gitranker.api.batch.reader;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class UserItemReader {
    private final UserRepository userRepository;

    public RepositoryItemReader<User> createReader(int pageSize) {
        RepositoryItemReader<User> reader = new RepositoryItemReader<>();

        reader.setRepository(userRepository);
        reader.setMethodName("findAll");
        reader.setPageSize(pageSize);
        reader.setSort(Collections.singletonMap("id", Sort.Direction.ASC));

        return reader;
    }
}
