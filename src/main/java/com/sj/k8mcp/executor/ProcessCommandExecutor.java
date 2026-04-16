package com.sj.k8mcp.executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class ProcessCommandExecutor implements CommandExecutor {

    @Value("${app.command.timeout-seconds:30}")
    private int defaultTimeoutSeconds;

    @Value("${app.command.max-output-chars:50000}")
    private int maxOutputChars;

    private final CommandGuard commandGuard;

    public ProcessCommandExecutor(CommandGuard commandGuard) {
        this.commandGuard = commandGuard;
    }

    @Override
    public CommandResult execute(List<String> command) {
        return execute(command, Duration.ofSeconds(defaultTimeoutSeconds));
    }

    @Override
    public CommandResult execute(List<String> command, Duration timeout) {
        try {
            commandGuard.validate(command);
        } catch (SecurityException e) {
            return new CommandResult("", "SECURITY VIOLATION: " + e.getMessage(), 126);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            // Use virtual threads (Java 21+) to read stdout and stderr concurrently,
            // preventing pipe buffer deadlock when one stream fills while the other is unread.
            try (ExecutorService ioPool = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<String> stdoutFuture = ioPool.submit(() -> readStream(process.getInputStream()));
                Future<String> stderrFuture = ioPool.submit(() -> readStream(process.getErrorStream()));

                boolean finished = process.waitFor(timeout.getSeconds(), TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return new CommandResult("", "Command timed out after " + timeout.getSeconds() + "s", 124);
                }

                String stdout = truncate(stdoutFuture.get(5, TimeUnit.SECONDS));
                String stderr = truncate(stderrFuture.get(5, TimeUnit.SECONDS));
                return new CommandResult(stdout, stderr, process.exitValue());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CommandResult("", "Command interrupted: " + e.getMessage(), 130);
        } catch (Exception e) {
            return new CommandResult("", "Command execution failed: " + e.getMessage(), 1);
        }
    }

    private String readStream(InputStream stream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "";
        }
    }

    private String truncate(String output) {
        if (output != null && output.length() > maxOutputChars) {
            return output.substring(0, maxOutputChars) + "\n[output truncated at " + maxOutputChars + " chars]";
        }
        return output;
    }
}
