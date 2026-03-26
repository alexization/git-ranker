package com.gitranker.api.testsupport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DockerPreflightCheckTest {

    @Test
    @DisplayName("docker daemon이 reachable이면 성공 결과를 반환한다")
    void should_returnSuccess_when_dockerDaemonIsReachable() {
        DockerPreflightCheck check = new DockerPreflightCheck(command -> {
            if (command.equals(List.of("docker", "context", "show"))) {
                return CommandResult.success("orbstack\n", "");
            }
            if (command.equals(List.of("docker", "version", "--format", "{{.Server.APIVersion}}"))) {
                return CommandResult.success("1.51\n", "");
            }
            throw new IllegalArgumentException("Unexpected command: " + command);
        });

        DockerPreflightResult result = check.run();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.message())
                .isEqualTo("Docker preflight passed. context=orbstack serverApiVersion=1.51");
    }

    @Test
    @DisplayName("docker daemon에 연결할 수 없으면 환경 문제를 설명하는 실패 결과를 반환한다")
    void should_returnHelpfulFailure_when_dockerDaemonIsUnavailable() {
        DockerPreflightCheck check = new DockerPreflightCheck(command -> {
            if (command.equals(List.of("docker", "context", "show"))) {
                return CommandResult.success("orbstack\n", "");
            }
            if (command.equals(List.of("docker", "version", "--format", "{{.Server.APIVersion}}"))) {
                return CommandResult.failure(
                        1,
                        "",
                        "Cannot connect to the Docker daemon at unix:///tmp/docker.sock. Is the docker daemon running?");
            }
            throw new IllegalArgumentException("Unexpected command: " + command);
        });

        DockerPreflightResult result = check.run();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.message()).contains("Docker preflight failed for Testcontainers integration tests.");
        assertThat(result.message()).contains("Cause: Docker daemon is not reachable from the current shell.");
        assertThat(result.message()).contains("Context: orbstack");
        assertThat(result.message()).contains("docker version");
        assertThat(result.message()).contains("./gradlew test jacocoTestCoverageVerification");
        assertThat(result.message()).contains("./gradlew integrationTest");
    }

    @Test
    @DisplayName("docker CLI가 없으면 설치 전제조건을 안내하는 실패 결과를 반환한다")
    void should_returnHelpfulFailure_when_dockerCliIsMissing() {
        DockerPreflightCheck check = new DockerPreflightCheck(command -> {
            if (command.equals(List.of("docker", "context", "show"))) {
                return CommandResult.failure(127, "", "docker: command not found");
            }
            if (command.equals(List.of("docker", "version", "--format", "{{.Server.APIVersion}}"))) {
                return CommandResult.failure(127, "", "docker: command not found");
            }
            throw new IllegalArgumentException("Unexpected command: " + command);
        });

        DockerPreflightResult result = check.run();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.message()).contains("Cause: Docker CLI is not installed or not available on PATH.");
        assertThat(result.message()).contains("Context: unavailable");
        assertThat(result.message()).contains("Install Docker Desktop, OrbStack, or another compatible Docker runtime.");
    }
}
