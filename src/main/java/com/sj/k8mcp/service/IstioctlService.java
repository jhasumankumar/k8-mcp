package com.sj.k8mcp.service;

import com.sj.k8mcp.executor.CommandExecutor;
import com.sj.k8mcp.executor.CommandResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IstioctlService {

    private final CommandExecutor commandExecutor;

    @Value("${app.istioctl.path:istioctl}")
    private String istioctlPath;

    public IstioctlService(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    public CommandResult proxyStatus(String namespace) {
        List<String> cmd = new ArrayList<>(List.of(istioctlPath, "proxy-status"));
        if (namespace != null && !namespace.isBlank()) {
            cmd.addAll(List.of("-n", namespace));
        }
        return commandExecutor.execute(cmd);
    }

    public CommandResult proxyConfigListener(String namespace, String podName) {
        return commandExecutor.execute(List.of(
                istioctlPath, "proxy-config", "listener", podRef(namespace, podName)
        ));
    }

    public CommandResult proxyConfigEndpoint(String namespace, String podName) {
        return commandExecutor.execute(List.of(
                istioctlPath, "proxy-config", "endpoint", podRef(namespace, podName)
        ));
    }

    public CommandResult proxyConfigOutboundClusters(String namespace, String podName) {
        return commandExecutor.execute(List.of(
                istioctlPath, "proxy-config", "cluster", podRef(namespace, podName),
                "--direction", "outbound"
        ));
    }

    public CommandResult proxyConfigOutboundRoutes(String namespace, String podName) {
        return commandExecutor.execute(List.of(
                istioctlPath, "proxy-config", "route", podRef(namespace, podName),
                "--direction", "outbound"
        ));
    }

    // istioctl proxy-config commands accept pod.namespace as a single argument
    private String podRef(String namespace, String podName) {
        if (namespace == null || namespace.isBlank()) {
            return podName;
        }
        return podName + "." + namespace;
    }
}
