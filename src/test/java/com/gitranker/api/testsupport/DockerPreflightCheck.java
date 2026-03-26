package com.gitranker.api.testsupport;

import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Objects;

public final class DockerPreflightCheck {

    private static final List<String> DOCKER_CONTEXT_COMMAND = List.of("docker", "context", "show");
    private static final List<String> DOCKER_VERSION_COMMAND =
            List.of("docker", "version", "--format", "{{.Server.APIVersion}}");

    private final CommandRunner commandRunner;

    public DockerPreflightCheck(CommandRunner commandRunner) {
        this.commandRunner = Objects.requireNonNull(commandRunner);
    }

    public DockerPreflightResult run() {
        CommandResult contextResult = commandRunner.run(DOCKER_CONTEXT_COMMAND);
        String context = normalizedOrFallback(contextResult.stdout(), "unavailable");

        CommandResult versionResult = commandRunner.run(DOCKER_VERSION_COMMAND);
        if (versionResult.isSuccess()) {
            String apiVersion = normalizedOrFallback(versionResult.stdout(), "unknown");
            return DockerPreflightResult.success(
                    "Docker preflight passed. context=%s serverApiVersion=%s".formatted(context, apiVersion));
        }

        return DockerPreflightResult.failure(buildFailureMessage(context, versionResult));
    }

    private String buildFailureMessage(String context, CommandResult versionResult) {
        String diagnostic = versionResult.primaryDiagnostic();
        String cause = determineCause(versionResult);
        String firstStep = isDockerCliMissing(versionResult)
                ? "- Install Docker Desktop, OrbStack, or another compatible Docker runtime."
                : "- Start Docker Desktop, OrbStack, or another compatible Docker runtime and confirm `docker version` succeeds.";

        return """
                Docker preflight failed for Testcontainers integration tests.
                Cause: %s
                Context: %s
                Details: %s
                Next steps:
                %s
                - If you only need code-level verification, run `./gradlew test jacocoTestCoverageVerification`.
                - Re-run `./gradlew integrationTest` after Docker is available.
                """.formatted(cause, context, diagnostic, firstStep);
    }

    private String determineCause(CommandResult versionResult) {
        if (isDockerCliMissing(versionResult)) {
            return "Docker CLI is not installed or not available on PATH.";
        }
        if (isDockerDaemonUnavailable(versionResult)) {
            return "Docker daemon is not reachable from the current shell.";
        }
        return "Docker returned a non-zero exit code before Testcontainers could start.";
    }

    private boolean isDockerCliMissing(CommandResult versionResult) {
        String diagnostic = versionResult.primaryDiagnostic().toLowerCase();
        return versionResult.exitCode() == 127
                || diagnostic.contains("command not found")
                || diagnostic.contains("no such file or directory");
    }

    private boolean isDockerDaemonUnavailable(CommandResult versionResult) {
        String diagnostic = versionResult.primaryDiagnostic().toLowerCase();
        return diagnostic.contains("cannot connect to the docker daemon")
                || diagnostic.contains("is the docker daemon running")
                || diagnostic.contains("error during connect");
    }

    private String normalizedOrFallback(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}

@FunctionalInterface
interface CommandRunner {

    CommandResult run(List<String> command);
}

record CommandResult(int exitCode, String stdout, String stderr) {

    static CommandResult success(String stdout, String stderr) {
        return new CommandResult(0, stdout, stderr);
    }

    static CommandResult failure(int exitCode, String stdout, String stderr) {
        if (exitCode == 0) {
            throw new IllegalArgumentException("Failure result must have non-zero exit code");
        }
        return new CommandResult(exitCode, stdout, stderr);
    }

    boolean isSuccess() {
        return exitCode == 0;
    }

    String primaryDiagnostic() {
        String stderrText = stderr == null ? "" : stderr.trim();
        if (!stderrText.isEmpty()) {
            return stderrText;
        }

        String stdoutText = stdout == null ? "" : stdout.trim();
        if (!stdoutText.isEmpty()) {
            return stdoutText;
        }

        return "No additional docker diagnostic output was produced.";
    }
}

record DockerPreflightResult(boolean isSuccess, String message) {

    static DockerPreflightResult success(String message) {
        return new DockerPreflightResult(true, message);
    }

    static DockerPreflightResult failure(String message) {
        return new DockerPreflightResult(false, message);
    }
}

final class ProcessCommandRunner implements CommandRunner {

    @Override
    public CommandResult run(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false);

        try {
            Process process = processBuilder.start();
            CompletableFuture<String> stdoutFuture =
                    CompletableFuture.supplyAsync(() -> readStream(process.getInputStream()));
            CompletableFuture<String> stderrFuture =
                    CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()));
            int exitCode = process.waitFor();
            String stdout = stdoutFuture.join();
            String stderr = stderrFuture.join();
            return new CommandResult(exitCode, stdout, stderr);
        } catch (IOException exception) {
            return CommandResult.failure(127, "", exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return CommandResult.failure(130, "", "Interrupted while running command: " + String.join(" ", command));
        }
    }

    private String readStream(InputStream inputStream) {
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
