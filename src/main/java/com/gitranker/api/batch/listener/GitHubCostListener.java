package com.gitranker.api.batch.listener;

import com.gitranker.api.global.logging.EventType;
import com.gitranker.api.global.logging.LogCategory;
import com.gitranker.api.global.logging.MdcUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GitHubCostListener implements JobExecutionListener {

    public static final String TOTAL_COST_KEY = "totalGitHubCost";

    @Override
    public void beforeJob(JobExecution jobExecution) {
        jobExecution.getExecutionContext().putInt(TOTAL_COST_KEY, 0);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        int totalCost = jobExecution.getExecutionContext().getInt(TOTAL_COST_KEY, 0);

        MdcUtils.setGithubApiCost(totalCost);
        MdcUtils.setLogContext(LogCategory.BATCH, EventType.SUCCESS);

        log.info("소요 GitHub API Cost: {}", totalCost);
    }
}
