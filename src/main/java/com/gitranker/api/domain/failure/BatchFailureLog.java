package com.gitranker.api.domain.failure;

import com.gitranker.api.global.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "batch_failure_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchFailureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobName;

    @Column(nullable = false)
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ErrorType errorType;

    @Column(length = 1000)
    private String errorMessage;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public BatchFailureLog(String jobName, String targetId, ErrorType errorType, String errorMessage) {
        this.jobName = jobName;
        this.targetId = targetId;
        this.errorType = errorType;
        this.errorMessage = (errorMessage != null && errorMessage.length() > 1000)
                ? errorMessage.substring(0, 1000)
                : errorMessage;
        this.createdAt = LocalDateTime.now();
    }
}
