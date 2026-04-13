package com.sj.k8mcp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("k8-mcp — Kubernetes MCP Server")
                        .version("1.0.0")
                        .description("""
                                MCP (Model Context Protocol) server that exposes **kubectl** and **istioctl** \
                                commands as callable tools for AI assistants (Claude, GitHub Copilot).

                                Connect any MCP-compatible AI assistant to this server to query and inspect \
                                your Kubernetes cluster and Istio service mesh using natural language.

                                **MCP SSE endpoint:** `GET /sse`
                                """)
                        .contact(new Contact()
                                .name("k8-mcp")
                                .url("https://github.com/jhasumankumar/k8-mcp")))
                .servers(List.of(
                        new Server().url("/").description("Current server")
                ));
    }
}
