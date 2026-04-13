package com.sj.k8mcp.service;

import com.sj.k8mcp.executor.CommandExecutor;
import com.sj.k8mcp.executor.CommandResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KubectlServiceTest {

    @Mock
    private CommandExecutor commandExecutor;

    @InjectMocks
    private KubectlService kubectlService;

    @Captor
    private ArgumentCaptor<List<String>> commandCaptor;

    private static final CommandResult SUCCESS = new CommandResult("output", "", 0);
    private static final CommandResult FAILURE = new CommandResult("", "error", 1);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kubectlService, "kubectlPath", "kubectl");
    }

    // --- getPods ---

    @Test
    void getPods_withNamespace_buildsCommandWithNamespaceFlag() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        kubectlService.getPods("default");

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .containsExactly("kubectl", "get", "pods", "-n", "default", "-o", "wide");
    }

    @Test
    void getPods_withNullNamespace_buildsCommandWithAllNamespacesFlag() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        kubectlService.getPods(null);

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .containsExactly("kubectl", "get", "pods", "--all-namespaces", "-o", "wide");
    }

    @Test
    void getPods_withBlankNamespace_treatsAsAllNamespaces() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        kubectlService.getPods("  ");

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).contains("--all-namespaces");
    }

    // --- getPodsByLabel ---

    @Test
    void getPodsByLabel_buildsCommandWithLabelSelector() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        kubectlService.getPodsByLabel("prod", "app=frontend,env=prod");

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .containsExactly("kubectl", "get", "pods", "-n", "prod", "-l", "app=frontend,env=prod", "-o", "wide");
    }

    // --- getPodLogs ---

    @Test
    void getPodLogs_usesTailFlagAndLongerTimeout() {
        when(commandExecutor.execute(anyList(), any(Duration.class))).thenReturn(SUCCESS);

        kubectlService.getPodLogs("default", "my-pod");

        verify(commandExecutor).execute(commandCaptor.capture(), any(Duration.class));
        assertThat(commandCaptor.getValue())
                .containsExactly("kubectl", "logs", "my-pod", "-n", "default", "--tail=100");
    }

    // --- describePod ---

    @Test
    void describePod_buildsCorrectCommand() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        kubectlService.describePod("kube-system", "coredns-abc");

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .containsExactly("kubectl", "describe", "pod", "coredns-abc", "-n", "kube-system");
    }

    // --- getPodsByNode ---

    @Test
    void getPodsByNode_usesFieldSelectorWithNodeName() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        kubectlService.getPodsByNode("worker-1");

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .containsExactly("kubectl", "get", "pods", "--all-namespaces",
                        "--field-selector", "spec.nodeName=worker-1", "-o", "wide");
    }

    // --- getPodsByStatus ---

    @Test
    void getPodsByStatus_buildsCommandWithPhaseFieldSelector() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        kubectlService.getPodsByStatus("default", "Failed");

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .containsExactly("kubectl", "get", "pods", "-n", "default",
                        "--field-selector", "status.phase=Failed");
    }

    // --- getIstioCrd ---

    @Test
    void getIstioCrd_virtualService_usesCorrectResourceNameAndYamlOutput() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        kubectlService.getIstioCrd("VirtualService", "istio-system");

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .containsExactly("kubectl", "get", "virtualservices", "-n", "istio-system", "-o", "yaml");
    }

    @Test
    void getIstioCrd_destinationRule_usesCorrectResourceName() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        kubectlService.getIstioCrd("DestinationRule", "prod");

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).contains("destinationrules");
    }

    @Test
    void getIstioCrd_unknownKind_returnsErrorResultWithoutCallingExecutor() {
        CommandResult result = kubectlService.getIstioCrd("UnknownCrd", "default");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.stderr()).contains("Unsupported CRD kind: UnknownCrd");
    }

    @Test
    void getIstioCrd_nullNamespace_usesAllNamespaces() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        kubectlService.getIstioCrd("EnvoyFilter", null);

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).contains("--all-namespaces");
    }
}
