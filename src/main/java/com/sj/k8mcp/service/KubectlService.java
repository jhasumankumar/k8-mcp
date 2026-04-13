package com.sj.k8mcp.service;

import com.sj.k8mcp.executor.CommandExecutor;
import com.sj.k8mcp.executor.CommandResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KubectlService {

    private static final Map<String, String> ISTIO_CRD_RESOURCES = Map.of(
            "ServiceEntry", "serviceentries",
            "VirtualService", "virtualservices",
            "DestinationRule", "destinationrules",
            "EnvoyFilter", "envoyfilters"
    );

    private final CommandExecutor commandExecutor;

    @Value("${app.kubectl.path:kubectl}")
    private String kubectlPath;

    public KubectlService(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    public CommandResult getPods(String namespace) {
        List<String> cmd = new ArrayList<>(List.of(kubectlPath, "get", "pods"));
        addNamespaceArgs(cmd, namespace);
        cmd.addAll(List.of("-o", "wide"));
        return commandExecutor.execute(cmd);
    }

    public CommandResult getPodsByLabel(String namespace, String labelSelector) {
        List<String> cmd = new ArrayList<>(List.of(kubectlPath, "get", "pods"));
        addNamespaceArgs(cmd, namespace);
        cmd.addAll(List.of("-l", labelSelector, "-o", "wide"));
        return commandExecutor.execute(cmd);
    }

    public CommandResult getNodes() {
        return commandExecutor.execute(List.of(kubectlPath, "get", "nodes", "-o", "wide"));
    }

    public CommandResult getPodLogs(String namespace, String podName) {
        List<String> cmd = new ArrayList<>(List.of(kubectlPath, "logs", podName));
        if (!isAllNamespaces(namespace)) {
            cmd.addAll(List.of("-n", namespace));
        }
        cmd.add("--tail=100");
        return commandExecutor.execute(cmd, Duration.ofSeconds(60));
    }

    public CommandResult describePod(String namespace, String podName) {
        List<String> cmd = new ArrayList<>(List.of(kubectlPath, "describe", "pod", podName));
        if (!isAllNamespaces(namespace)) {
            cmd.addAll(List.of("-n", namespace));
        }
        return commandExecutor.execute(cmd);
    }

    public CommandResult describeNode(String nodeName) {
        return commandExecutor.execute(List.of(kubectlPath, "describe", "node", nodeName));
    }

    public CommandResult getConfigMaps(String namespace) {
        List<String> cmd = new ArrayList<>(List.of(kubectlPath, "get", "configmaps"));
        addNamespaceArgs(cmd, namespace);
        return commandExecutor.execute(cmd);
    }

    public CommandResult getPodsByNode(String nodeName) {
        return commandExecutor.execute(List.of(
                kubectlPath, "get", "pods",
                "--all-namespaces",
                "--field-selector", "spec.nodeName=" + nodeName,
                "-o", "wide"
        ));
    }

    public CommandResult getPodsByStatus(String namespace, String status) {
        List<String> cmd = new ArrayList<>(List.of(kubectlPath, "get", "pods"));
        addNamespaceArgs(cmd, namespace);
        cmd.addAll(List.of("--field-selector", "status.phase=" + status));
        return commandExecutor.execute(cmd);
    }

    public CommandResult getIstioCrd(String crdKind, String namespace) {
        String resourceName = ISTIO_CRD_RESOURCES.get(crdKind);
        if (resourceName == null) {
            String supported = String.join(", ", ISTIO_CRD_RESOURCES.keySet());
            return new CommandResult("", "Unsupported CRD kind: " + crdKind + ". Supported: " + supported, 1);
        }
        List<String> cmd = new ArrayList<>(List.of(kubectlPath, "get", resourceName));
        addNamespaceArgs(cmd, namespace);
        cmd.addAll(List.of("-o", "yaml"));
        return commandExecutor.execute(cmd);
    }

    private void addNamespaceArgs(List<String> cmd, String namespace) {
        if (isAllNamespaces(namespace)) {
            cmd.add("--all-namespaces");
        } else {
            cmd.addAll(List.of("-n", namespace));
        }
    }

    private boolean isAllNamespaces(String namespace) {
        return namespace == null || namespace.isBlank();
    }
}
