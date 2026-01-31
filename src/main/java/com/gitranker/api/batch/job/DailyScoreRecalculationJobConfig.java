package com.gitranker.api.batch.job;

import com.gitranker.api.batch.listener.BatchProgressListener;
import com.gitranker.api.batch.listener.GitHubCostListener;
import com.gitranker.api.batch.listener.UserScoreCalculationSkipListener;
import com.gitranker.api.batch.processor.ScoreRecalculationProcessor;
import com.gitranker.api.batch.reader.UserItemReader;
import com.gitranker.api.batch.tasklet.RankingRecalculationTasklet;
import com.gitranker.api.batch.writer.UserItemWriter;
import com.gitranker.api.domain.user.User;
import com.gitranker.api.global.error.exception.GitHubApiNonRetryableException;
import com.gitranker.api.global.error.exception.GitHubApiRetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DailyScoreRecalculationJobConfig {

    @Value("${batch.chunk-size:100}")
    private int chunkSize;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final UserItemReader userItemReader;
    private final ScoreRecalculationProcessor scoreRecalculationProcessor;
    private final RankingRecalculationTasklet rankingRecalculationTasklet;
    private final UserScoreCalculationSkipListener userScoreCalculationSkipListener;
    private final UserItemWriter userItemWriter;
    private final GitHubCostListener gitHubCostListener;
    private final BatchProgressListener batchProgressListener;

    @Bean
    public Job dailyScoreRecalculationJob() {
        return new JobBuilder("dailyScoreRecalculationJob", jobRepository)
                .listener(gitHubCostListener)
                .start(scoreRecalculationStep())
                .next(rankingRecalculationStep())
                .build();
    }

    @Bean
    public Step scoreRecalculationStep() {
        return new StepBuilder("scoreRecalculationStep", jobRepository)
                .<User, User>chunk(chunkSize, transactionManager)
                .reader(userItemReader.createReader(chunkSize))
                .processor(scoreRecalculationProcessor)
                .writer(userItemWriter)
                .faultTolerant()
                .retry(GitHubApiRetryableException.class)
                .retryLimit(3)
                .backOffPolicy(new ExponentialBackOffPolicy())
                .skip(GitHubApiNonRetryableException.class)
                .skip(GitHubApiRetryableException.class)
                .skipLimit(100)
                .listener(userScoreCalculationSkipListener)
                .listener(batchProgressListener)
                .build();
    }

    @Bean
    public Step rankingRecalculationStep() {
        return new StepBuilder("rankingRecalculationStep", jobRepository)
                .tasklet(rankingRecalculationTasklet, transactionManager)
                .build();
    }
}
