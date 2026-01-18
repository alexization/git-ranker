package com.gitranker.api.batch;

import com.gitranker.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job dailyScoreRecalculationJob;

    @PatchMapping("/run/daily-job")
    public ApiResponse<String> runDailyScoreRecalculationJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLocalDateTime("runTime", LocalDateTime.now())
                .toJobParameters();

        jobLauncher.run(dailyScoreRecalculationJob, params);

        return ApiResponse.success("Daily Score Recalculation Job 실행 완료");
    }
}
