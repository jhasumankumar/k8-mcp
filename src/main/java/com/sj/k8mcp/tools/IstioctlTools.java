package com.sj.k8mcp.tools;

import com.sj.k8mcp.service.IstioctlService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class IstioctlTools {

    private final IstioctlService istioctlService;

    public IstioctlTools(IstioctlService istioctlService) {
        this.istioctlService = istioctlService;
    }

    @Tool(description = """
            Show the sync status of all Envoy proxies in the Istio mesh, or filtered by namespace.
            Displays xDS sync state (CDS/LDS/EDS/RDS/ECDS) for each pod alongside the istiod version.
            Use to detect out-of-sync Envoy sidecars or mismatches between the proxy and control plane versions.
            """)
    public String proxyStatus(
            @ToolParam(description = "Kubernetes namespace to filter by. Pass null to show all namespaces.", required = false)
            String namespace) {
        return istioctlService.proxyStatus(namespace).toFormattedString();
    }

    @Tool(description = """
            Show the Envoy listener configuration for a specific pod's sidecar proxy.
            Reveals all inbound and outbound listeners configured by Istio's control plane,
            including addresses, ports, filter chains, and bound routes.
            Use to verify that traffic interception is configured correctly for a pod.
            """)
    public String proxyConfigListener(
            @ToolParam(description = "Kubernetes namespace the pod belongs to.")
            String namespace,
            @ToolParam(description = "Name of the pod whose Envoy listener config to inspect.")
            String podName) {
        return istioctlService.proxyConfigListener(namespace, podName).toFormattedString();
    }

    @Tool(description = """
            Show the Envoy endpoint configuration for a specific pod's sidecar proxy.
            Lists all upstream endpoints Envoy knows about, including their addresses, ports, health status, and locality.
            Use to verify service discovery is working and that expected backends are visible to a pod.
            """)
    public String proxyConfigEndpoint(
            @ToolParam(description = "Kubernetes namespace the pod belongs to.")
            String namespace,
            @ToolParam(description = "Name of the pod whose Envoy endpoint config to inspect.")
            String podName) {
        return istioctlService.proxyConfigEndpoint(namespace, podName).toFormattedString();
    }

    @Tool(description = """
            Show the outbound cluster configuration for a specific pod's Envoy sidecar.
            Clusters correspond to upstream services. Each entry shows circuit-breaker settings,
            timeout policies, TLS mode, and load balancing configuration applied by Istio.
            Use to verify that DestinationRule policies are being applied correctly for outbound traffic.
            """)
    public String proxyConfigOutboundClusters(
            @ToolParam(description = "Kubernetes namespace the pod belongs to.")
            String namespace,
            @ToolParam(description = "Name of the pod whose outbound cluster config to inspect.")
            String podName) {
        return istioctlService.proxyConfigOutboundClusters(namespace, podName).toFormattedString();
    }

    @Tool(description = """
            Show the outbound route configuration for a specific pod's Envoy sidecar.
            Routes determine how Envoy forwards outbound HTTP requests to clusters.
            Reflects the effective result of VirtualService traffic routing rules applied by Istio.
            Use to debug routing decisions, traffic splits, header-based routing, or retries.
            """)
    public String proxyConfigOutboundRoutes(
            @ToolParam(description = "Kubernetes namespace the pod belongs to.")
            String namespace,
            @ToolParam(description = "Name of the pod whose outbound route config to inspect.")
            String podName) {
        return istioctlService.proxyConfigOutboundRoutes(namespace, podName).toFormattedString();
    }
}
