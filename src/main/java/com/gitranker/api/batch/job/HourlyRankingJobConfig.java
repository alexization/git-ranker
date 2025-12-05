package com.gitranker.api.batch.job;

import com.gitranker.api.batch.tasklet.RankingRecalculationTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class HourlyRankingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RankingRecalculationTasklet rankingRecalculationTasklet;

    @Bean
    public Job hourlyRankingJob() {
        return new JobBuilder("hourlyRankingJob", jobRepository)
                .start(hourlyRankingStep())
                .build();
    }

    @Bean
    public Step hourlyRankingStep() {
        return new StepBuilder("hourlyRankingStep", jobRepository)
                .tasklet(rankingRecalculationTasklet, transactionManager)
                .build();
    }
}
