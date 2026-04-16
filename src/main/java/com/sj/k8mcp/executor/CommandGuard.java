package com.sj.k8mcp.executor;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Security guard that enforces an allowlist of read-only subcommands before any subprocess is spawned.
 * Blocks write/destructive operations regardless of how the command was constructed.
 * Exit code 126 mirrors the Unix "permission denied" convention.
 */
@Component
public class CommandGuard {

    private static final Set<String> KUBECTL_ALLOWED = Set.of(
            "get", "describe", "logs", "top", "version",
            "cluster-info", "api-resources", "api-versions", "explain"
    );

    private static final Set<String> ISTIOCTL_ALLOWED = Set.of(
            "proxy-status", "proxy-config", "analyze", "version", "verify-install"
    );

    private static final Map<String, Set<String>> ALLOWED_BY_BINARY = Map.of(
            "kubectl",   KUBECTL_ALLOWED,
            "istioctl",  ISTIOCTL_ALLOWED
    );

    /**
     * Validates that {@code command} is an allowed read-only invocation.
     *
     * @throws SecurityException if the binary or subcommand is not on the allowlist
     */
    public void validate(List<String> command) {
        if (command == null || command.isEmpty()) {
            throw new SecurityException("Empty command is not permitted");
        }

        // Extract the bare binary name so configured absolute paths still match
        String binaryName = Path.of(command.get(0)).getFileName().toString();
        Set<String> allowed = ALLOWED_BY_BINARY.get(binaryName);
        if (allowed == null) {
            throw new SecurityException(
                    "Unauthorized binary '" + binaryName + "'. Only kubectl and istioctl are permitted.");
        }

        if (command.size() < 2) {
            throw new SecurityException(
                    "Command must include a subcommand after '" + binaryName + "'");
        }

        String subcommand = command.get(1);
        if (!allowed.contains(subcommand)) {
            throw new SecurityException(
                    "Unauthorized subcommand '" + subcommand + "' for " + binaryName +
                    ". Allowed read-only subcommands: " + allowed);
        }
    }
}
