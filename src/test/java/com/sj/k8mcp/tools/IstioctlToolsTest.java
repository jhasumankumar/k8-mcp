package com.sj.k8mcp.tools;

import com.sj.k8mcp.executor.CommandResult;
import com.sj.k8mcp.service.IstioctlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IstioctlToolsTest {

    @Mock
    private IstioctlService istioctlService;

    @InjectMocks
    private IstioctlTools istioctlTools;

    // --- proxyStatus ---

    @Test
    void proxyStatus_delegatesToServiceAndReturnsOutput() {
        when(istioctlService.proxyStatus("prod"))
                .thenReturn(new CommandResult("NAME   CDS   LDS   EDS   RDS   ECDS   ISTIOD\nmy-pod.prod   SYNCED", "", 0));

        String result = istioctlTools.proxyStatus("prod");

        verify(istioctlService).proxyStatus("prod");
        assertThat(result).contains("SYNCED");
    }

    @Test
    void proxyStatus_withNullNamespace_delegatesNull() {
        when(istioctlService.proxyStatus(null))
                .thenReturn(new CommandResult("all-namespace-proxies", "", 0));

        String result = istioctlTools.proxyStatus(null);

        verify(istioctlService).proxyStatus(null);
        assertThat(result).isEqualTo("all-namespace-proxies");
    }

    @Test
    void proxyStatus_onFailure_returnsErrorMessage() {
        when(istioctlService.proxyStatus("prod"))
                .thenReturn(new CommandResult("", "istiod not reachable", 1));

        String result = istioctlTools.proxyStatus("prod");

        assertThat(result).contains("ERROR (exit code 1)");
        assertThat(result).contains("istiod not reachable");
    }

    // --- proxyConfigListener ---

    @Test
    void proxyConfigListener_delegatesNamespaceAndPodName() {
        when(istioctlService.proxyConfigListener("prod", "api-pod"))
                .thenReturn(new CommandResult("ADDRESS   PORT   TYPE\n0.0.0.0   80   HTTP", "", 0));

        String result = istioctlTools.proxyConfigListener("prod", "api-pod");

        verify(istioctlService).proxyConfigListener("prod", "api-pod");
        assertThat(result).contains("0.0.0.0");
    }

    // --- proxyConfigEndpoint ---

    @Test
    void proxyConfigEndpoint_delegatesNamespaceAndPodName() {
        when(istioctlService.proxyConfigEndpoint("staging", "frontend"))
                .thenReturn(new CommandResult("ENDPOINT   STATUS   OUTLIER\n10.0.0.1:8080   HEALTHY   OK", "", 0));

        String result = istioctlTools.proxyConfigEndpoint("staging", "frontend");

        verify(istioctlService).proxyConfigEndpoint("staging", "frontend");
        assertThat(result).contains("HEALTHY");
    }

    // --- proxyConfigOutboundClusters ---

    @Test
    void proxyConfigOutboundClusters_delegatesAndReturnsOutput() {
        when(istioctlService.proxyConfigOutboundClusters("default", "checkout"))
                .thenReturn(new CommandResult("SERVICE FQDN   PORT   SUBSET   DIRECTION\npayment-svc   443   -   outbound", "", 0));

        String result = istioctlTools.proxyConfigOutboundClusters("default", "checkout");

        verify(istioctlService).proxyConfigOutboundClusters("default", "checkout");
        assertThat(result).contains("outbound");
    }

    // --- proxyConfigOutboundRoutes ---

    @Test
    void proxyConfigOutboundRoutes_delegatesAndReturnsOutput() {
        when(istioctlService.proxyConfigOutboundRoutes("prod", "gateway"))
                .thenReturn(new CommandResult("NOTE: There are no route name   VIRTUAL HOSTS\nroute-abc   3", "", 0));

        String result = istioctlTools.proxyConfigOutboundRoutes("prod", "gateway");

        verify(istioctlService).proxyConfigOutboundRoutes("prod", "gateway");
        assertThat(result).contains("route-abc");
    }
}
