package com.sj.k8mcp.executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessCommandExecutorTest {

    private ProcessCommandExecutor executor;

    @BeforeEach
    void setUp() {
        // No-op guard: executor tests cover execution mechanics only; guard logic is tested in CommandGuardTest
        executor = new ProcessCommandExecutor(new CommandGuard() {
            @Override public void validate(java.util.List<String> cmd) {}
        });
        ReflectionTestUtils.setField(executor, "defaultTimeoutSeconds", 30);
        ReflectionTestUtils.setField(executor, "maxOutputChars", 50000);
    }

    @Test
    void execute_successfulCommand_returnsZeroExitCodeAndStdout() {
        CommandResult result = executor.execute(List.of("echo", "hello"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("hello");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void execute_failingCommand_returnsNonZeroExitCode() {
        // 'false' is a standard Unix command that always exits with code 1
        CommandResult result = executor.execute(List.of("false"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.exitCode()).isNotZero();
    }

    @Test
    void execute_nonExistentBinary_returnsErrorResult() {
        CommandResult result = executor.execute(List.of("this-binary-does-not-exist-xyz"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.stderr()).contains("Command execution failed");
    }

    @Test
    void execute_commandWithTimeout_timesOutAndReturnsExitCode124() {
        CommandResult result = executor.execute(List.of("sleep", "10"), Duration.ofMillis(200));

        assertThat(result.exitCode()).isEqualTo(124);
        assertThat(result.stderr()).contains("timed out");
    }

    @Test
    void execute_outputExceedsMaxChars_isTruncated() {
        ReflectionTestUtils.setField(executor, "maxOutputChars", 5);
        // 'echo' produces more than 5 chars
        CommandResult result = executor.execute(List.of("echo", "hello world"));

        assertThat(result.stdout()).contains("[output truncated at 5 chars]");
    }

    @Test
    void execute_commandWithStderr_capturesStderrSeparately() {
        // 'ls' on a non-existent path writes to stderr and exits non-zero
        CommandResult result = executor.execute(List.of("ls", "/this/path/does/not/exist/xyz"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.stderr()).isNotBlank();
    }

    @Test
    void toFormattedString_onSuccess_returnsStdout() {
        CommandResult result = executor.execute(List.of("echo", "output text"));

        assertThat(result.toFormattedString()).contains("output text");
    }

    @Test
    void toFormattedString_onFailure_includesExitCodeAndStderr() {
        CommandResult result = executor.execute(List.of("ls", "/nonexistent/path/xyz"));

        assertThat(result.toFormattedString()).contains("ERROR (exit code");
    }
}
