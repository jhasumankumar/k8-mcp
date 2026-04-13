package com.sj.k8mcp.config;

import com.sj.k8mcp.tools.IstioctlTools;
import com.sj.k8mcp.tools.KubectlTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolsConfig {

    @Bean
    public ToolCallbackProvider kubectlToolCallbackProvider(KubectlTools kubectlTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(kubectlTools)
                .build();
    }

    @Bean
    public ToolCallbackProvider istioctlToolCallbackProvider(IstioctlTools istioctlTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(istioctlTools)
                .build();
    }
}
