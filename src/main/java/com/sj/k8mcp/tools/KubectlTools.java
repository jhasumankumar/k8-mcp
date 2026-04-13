package com.sj.k8mcp.tools;

import com.sj.k8mcp.service.KubectlService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class KubectlTools {

    private final KubectlService kubectlService;

    public KubectlTools(KubectlService kubectlService) {
        this.kubectlService = kubectlService;
    }

    @Tool(description = """
            Get all pods in a Kubernetes namespace or across all namespaces.
            Returns pod name, ready status, restart count, age, IP, and node assignment in wide format.
            Pass namespace=null or omit it to get a cluster-wide view across all namespaces.
            """)
    public String getPods(
            @ToolParam(description = "Kubernetes namespace (e.g. 'default', 'kube-system'). Pass null for all namespaces.", required = false)
            String namespace) {
        return kubectlService.getPods(namespace).toFormattedString();
    }

    @Tool(description = """
            Get pods matching a label selector in a namespace or cluster-wide.
            Use Kubernetes label selector syntax: 'app=my-service', 'app=frontend,env=prod', or 'app in (a,b)'.
            Returns pod name, ready status, restart count, age, IP, and node assignment.
            """)
    public String getPodsByLabel(
            @ToolParam(description = "Kubernetes namespace. Pass null for all namespaces.", required = false)
            String namespace,
            @ToolParam(description = "Label selector, e.g. 'app=frontend' or 'app=api,env=prod'.")
            String labelSelector) {
        return kubectlService.getPodsByLabel(namespace, labelSelector).toFormattedString();
    }

    @Tool(description = """
            Get all Kubernetes nodes and their status.
            Returns node name, status, roles, age, Kubernetes version, OS image, kernel version, and container runtime.
            Useful for checking cluster capacity and identifying node problems.
            """)
    public String getNodes() {
        return kubectlService.getNodes().toFormattedString();
    }

    @Tool(description = """
            Get the last 100 log lines from a specific pod in a namespace.
            Useful for diagnosing recent errors, startup failures, or unexpected behaviour.
            For multi-container pods, this returns logs from the first container; use describePod to identify container names.
            """)
    public String getPodLogs(
            @ToolParam(description = "Kubernetes namespace the pod belongs to.")
            String namespace,
            @ToolParam(description = "Name of the pod to retrieve logs from.")
            String podName) {
        return kubectlService.getPodLogs(namespace, podName).toFormattedString();
    }

    @Tool(description = """
            Describe a pod in detail: events, resource requests/limits, environment variables, volume mounts, and conditions.
            Essential for diagnosing CrashLoopBackOff, Pending, OOMKilled, or ImagePullBackOff states.
            The Events section at the bottom is usually the most useful for diagnosis.
            """)
    public String describePod(
            @ToolParam(description = "Kubernetes namespace the pod belongs to.")
            String namespace,
            @ToolParam(description = "Name of the pod to describe.")
            String podName) {
        return kubectlService.describePod(namespace, podName).toFormattedString();
    }

    @Tool(description = """
            Describe a Kubernetes node in detail: capacity, allocatable resources, conditions, system info, taints, and running pods.
            Use to diagnose node pressure (MemoryPressure, DiskPressure), scheduling issues, or to verify node configuration.
            """)
    public String describeNode(
            @ToolParam(description = "Name of the Kubernetes node to describe.")
            String nodeName) {
        return kubectlService.describeNode(nodeName).toFormattedString();
    }

    @Tool(description = """
            List all ConfigMaps in a namespace or across all namespaces.
            Returns names, data keys, and age. Use namespace=null for a cluster-wide listing.
            """)
    public String getConfigMaps(
            @ToolParam(description = "Kubernetes namespace. Pass null for all namespaces.", required = false)
            String namespace) {
        return kubectlService.getConfigMaps(namespace).toFormattedString();
    }

    @Tool(description = """
            Get all pods scheduled on a specific node, across all namespaces.
            Useful when a node is under pressure, being drained, or when investigating workload distribution.
            Returns namespace, pod name, ready status, status, restarts, age, IP, and node.
            """)
    public String getPodsByNode(
            @ToolParam(description = "Name of the Kubernetes node to list pods for.")
            String nodeName) {
        return kubectlService.getPodsByNode(nodeName).toFormattedString();
    }

    @Tool(description = """
            Get pods filtered by their lifecycle phase/status in a namespace or cluster-wide.
            Valid status values: Running, Pending, Failed, Succeeded, Unknown.
            Use to quickly find all failed or stuck pods across the cluster.
            """)
    public String getPodsByStatus(
            @ToolParam(description = "Kubernetes namespace. Pass null for all namespaces.", required = false)
            String namespace,
            @ToolParam(description = "Pod phase to filter by: Running, Pending, Failed, Succeeded, or Unknown.")
            String status) {
        return kubectlService.getPodsByStatus(namespace, status).toFormattedString();
    }

    @Tool(description = """
            Get Istio custom resources (CRDs) in a namespace or cluster-wide. Returns YAML for detailed inspection.
            Supported kinds:
              - ServiceEntry: external service registrations
              - VirtualService: traffic routing rules
              - DestinationRule: load balancing, circuit breaker, and TLS policies
              - EnvoyFilter: low-level Envoy proxy configuration patches
            """)
    public String getIstioCrd(
            @ToolParam(description = "Istio CRD kind: ServiceEntry, VirtualService, DestinationRule, or EnvoyFilter.")
            String crdKind,
            @ToolParam(description = "Kubernetes namespace. Pass null for all namespaces.", required = false)
            String namespace) {
        return kubectlService.getIstioCrd(crdKind, namespace).toFormattedString();
    }
}
