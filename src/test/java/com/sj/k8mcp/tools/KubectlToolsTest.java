package com.sj.k8mcp.tools;

import com.sj.k8mcp.executor.CommandResult;
import com.sj.k8mcp.service.KubectlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KubectlToolsTest {

    @Mock
    private KubectlService kubectlService;

    @InjectMocks
    private KubectlTools kubectlTools;

    // --- getPods ---

    @Test
    void getPods_delegatesToServiceAndReturnsFormattedOutput() {
        when(kubectlService.getPods("default")).thenReturn(new CommandResult("pod-a   Running", "", 0));

        String result = kubectlTools.getPods("default");

        verify(kubectlService).getPods("default");
        assertThat(result).isEqualTo("pod-a   Running");
    }

    @Test
    void getPods_onFailure_returnsErrorMessage() {
        when(kubectlService.getPods("missing-ns"))
                .thenReturn(new CommandResult("", "namespace not found", 1));

        String result = kubectlTools.getPods("missing-ns");

        assertThat(result).contains("ERROR (exit code 1)");
        assertThat(result).contains("namespace not found");
    }

    // --- getPodLogs ---

    @Test
    void getPodLogs_delegatesToServiceWithNamespaceAndPodName() {
        when(kubectlService.getPodLogs("prod", "api-gateway-xyz"))
                .thenReturn(new CommandResult("log line 1\nlog line 2", "", 0));

        String result = kubectlTools.getPodLogs("prod", "api-gateway-xyz");

        verify(kubectlService).getPodLogs("prod", "api-gateway-xyz");
        assertThat(result).contains("log line 1");
        assertThat(result).contains("log line 2");
    }

    // --- describePod ---

    @Test
    void describePod_delegatesToServiceAndReturnsOutput() {
        when(kubectlService.describePod("default", "my-pod"))
                .thenReturn(new CommandResult("Name: my-pod\nNamespace: default", "", 0));

        String result = kubectlTools.describePod("default", "my-pod");

        verify(kubectlService).describePod("default", "my-pod");
        assertThat(result).contains("Name: my-pod");
    }

    // --- getNodes ---

    @Test
    void getNodes_delegatesToServiceWithNoArguments() {
        when(kubectlService.getNodes())
                .thenReturn(new CommandResult("worker-1   Ready", "", 0));

        String result = kubectlTools.getNodes();

        verify(kubectlService).getNodes();
        assertThat(result).contains("worker-1");
    }

    // --- getIstioCrd ---

    @Test
    void getIstioCrd_delegatesWithKindAndNamespace() {
        when(kubectlService.getIstioCrd("VirtualService", "prod"))
                .thenReturn(new CommandResult("apiVersion: networking.istio.io/v1alpha3", "", 0));

        String result = kubectlTools.getIstioCrd("VirtualService", "prod");

        verify(kubectlService).getIstioCrd("VirtualService", "prod");
        assertThat(result).contains("networking.istio.io");
    }

    @Test
    void getIstioCrd_onUnsupportedKind_returnsErrorFromService() {
        when(kubectlService.getIstioCrd("BadKind", "default"))
                .thenReturn(new CommandResult("", "Unsupported CRD kind: BadKind", 1));

        String result = kubectlTools.getIstioCrd("BadKind", "default");

        assertThat(result).contains("ERROR");
        assertThat(result).contains("Unsupported CRD kind");
    }

    // --- getPodsByNode ---

    @Test
    void getPodsByNode_delegatesNodeName() {
        when(kubectlService.getPodsByNode("worker-2"))
                .thenReturn(new CommandResult("default   nginx-abc   Running", "", 0));

        String result = kubectlTools.getPodsByNode("worker-2");

        verify(kubectlService).getPodsByNode("worker-2");
        assertThat(result).contains("nginx-abc");
    }

    // --- getPodsByStatus ---

    @Test
    void getPodsByStatus_delegatesNamespaceAndStatus() {
        when(kubectlService.getPodsByStatus(null, "Failed"))
                .thenReturn(new CommandResult("kube-system   broken-pod   Failed", "", 0));

        String result = kubectlTools.getPodsByStatus(null, "Failed");

        verify(kubectlService).getPodsByStatus(null, "Failed");
        assertThat(result).contains("broken-pod");
    }
}
