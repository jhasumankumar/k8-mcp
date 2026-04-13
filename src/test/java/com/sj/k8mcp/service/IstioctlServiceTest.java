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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IstioctlServiceTest {

    @Mock
    private CommandExecutor commandExecutor;

    @InjectMocks
    private IstioctlService istioctlService;

    @Captor
    private ArgumentCaptor<List<String>> commandCaptor;

    private static final CommandResult SUCCESS = new CommandResult("output", "", 0);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(istioctlService, "istioctlPath", "istioctl");
    }

    // --- proxyStatus ---

    @Test
    void proxyStatus_withNamespace_includesNamespaceFlag() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        istioctlService.proxyStatus("prod");

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .containsExactly("istioctl", "proxy-status", "-n", "prod");
    }

    @Test
    void proxyStatus_withNullNamespace_omitsNamespaceFlag() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        istioctlService.proxyStatus(null);

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .containsExactly("istioctl", "proxy-status");
    }

    // --- proxyConfigListener ---

    @Test
    void proxyConfigListener_usesDotNotationForPodRef() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        istioctlService.proxyConfigListener("prod", "my-pod");

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .containsExactly("istioctl", "proxy-config", "listener", "my-pod.prod");
    }

    // --- proxyConfigEndpoint ---

    @Test
    void proxyConfigEndpoint_usesDotNotationForPodRef() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        istioctlService.proxyConfigEndpoint("payments", "checkout-pod");

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .containsExactly("istioctl", "proxy-config", "endpoint", "checkout-pod.payments");
    }

    // --- proxyConfigOutboundClusters ---

    @Test
    void proxyConfigOutboundClusters_includesDirectionOutbound() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        istioctlService.proxyConfigOutboundClusters("default", "api-pod");

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .containsExactly("istioctl", "proxy-config", "cluster", "api-pod.default", "--direction", "outbound");
    }

    // --- proxyConfigOutboundRoutes ---

    @Test
    void proxyConfigOutboundRoutes_includesDirectionOutbound() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        istioctlService.proxyConfigOutboundRoutes("staging", "frontend-pod");

        verify(commandExecutor).execute(commandCaptor.capture());
        assertThat(commandCaptor.getValue())
                .containsExactly("istioctl", "proxy-config", "route", "frontend-pod.staging", "--direction", "outbound");
    }

    @Test
    void proxyConfigListener_withBlankNamespace_usesPodNameOnly() {
        when(commandExecutor.execute(anyList())).thenReturn(SUCCESS);

        istioctlService.proxyConfigListener("", "my-pod");

        verify(commandExecutor).execute(commandCaptor.capture());
        // blank namespace → pod name without dot-namespace suffix
        assertThat(commandCaptor.getValue()).contains("my-pod");
        assertThat(commandCaptor.getValue()).doesNotContain("my-pod.");
    }
}
