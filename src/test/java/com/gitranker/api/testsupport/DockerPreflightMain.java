package com.gitranker.api.testsupport;

public final class DockerPreflightMain {

    private DockerPreflightMain() {
    }

    public static void main(String[] args) {
        DockerPreflightResult result = new DockerPreflightCheck(new ProcessCommandRunner()).run();
        if (result.isSuccess()) {
            System.out.println(result.message());
            return;
        }

        System.err.println(result.message());
        System.exit(1);
    }
}
