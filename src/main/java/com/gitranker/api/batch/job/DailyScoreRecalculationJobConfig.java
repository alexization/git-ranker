package com.gitranker.api.batch.job;

import com.gitranker.api.batch.processor.ScoreRecalculationProcessor;
import com.gitranker.api.batch.reader.UserItemReader;
import com.gitranker.api.batch.tasklet.RankingRecalculationTasklet;
import com.gitranker.api.batch.writer.UserItemWriter;
import com.gitranker.api.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DailyScoreRecalculationJobConfig {
    private static final int CHUNK_SIZE = 50;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final UserItemReader userItemReader;
    private final ScoreRecalculationProcessor scoreRecalculationProcessor;
    private final RankingRecalculationTasklet rankingRecalculationTasklet;
    private final UserItemWriter userItemWriter;

    @Bean
    public Job dailyScoreRecalculationJob() {
        return new JobBuilder("dailyScoreRecalculationJob", jobRepository)
                .start(scoreRecalculationStep())
                .next(rankingRecalculationStep())
                .build();
    }

    @Bean
    public Step scoreRecalculationStep() {
        return new StepBuilder("scoreRecalculationStep", jobRepository)
                .<User, User>chunk(CHUNK_SIZE, transactionManager)
                .reader(userItemReader.createReader(CHUNK_SIZE))
                .processor(scoreRecalculationProcessor)
                .writer(userItemWriter)
                .build();
    }

    @Bean
    public Step rankingRecalculationStep() {
        return new StepBuilder("rankingRecalculationStep", jobRepository)
                .tasklet(rankingRecalculationTasklet, transactionManager)
                .build();
    }
}
