package com.sj.k8mcp.executor;

public record CommandResult(String stdout, String stderr, int exitCode) {

    public boolean isSuccess() {
        return exitCode == 0;
    }

    public String toFormattedString() {
        if (isSuccess()) {
            return stdout.isBlank() ? "(no output)" : stdout;
        }
        String errorBody = stderr.isBlank() ? stdout : stderr;
        return "ERROR (exit code " + exitCode + "):\n" + errorBody;
    }
}
