package com.sj.k8mcp.executor;

import java.time.Duration;
import java.util.List;

public interface CommandExecutor {

    CommandResult execute(List<String> command);

    CommandResult execute(List<String> command, Duration timeout);
}
