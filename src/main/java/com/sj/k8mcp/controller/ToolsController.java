package com.sj.k8mcp.controller;

import com.sj.k8mcp.model.ToolInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/tools")
@Tag(name = "MCP Tools", description = "Inspect all tools registered in the MCP server — their names, descriptions, and JSON Schema input definitions.")
public class ToolsController {

    private final List<ToolCallbackProvider> toolCallbackProviders;

    public ToolsController(List<ToolCallbackProvider> toolCallbackProviders) {
        this.toolCallbackProviders = toolCallbackProviders;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "List all registered MCP tools",
            description = "Returns metadata for every tool registered with the MCP server, including name, description, and the JSON Schema for its input parameters.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tool list retrieved successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ToolInfo.class)))
            }
    )
    public Flux<ToolInfo> listTools() {
        return Flux.fromIterable(toolCallbackProviders)
                .flatMapIterable(provider -> Arrays.asList(provider.getToolCallbacks()))
                .map(callback -> new ToolInfo(
                        callback.getToolDefinition().name(),
                        callback.getToolDefinition().description(),
                        callback.getToolDefinition().inputSchema()
                ));
    }

    @GetMapping(value = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get a specific MCP tool by name",
            description = "Returns the metadata for a single tool identified by its registered name.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tool found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ToolInfo.class))),
                    @ApiResponse(responseCode = "404", description = "Tool not found")
            }
    )
    public Mono<ToolInfo> getTool(
            @Parameter(description = "Registered tool name, e.g. getPods, proxyStatus", example = "getPods")
            @PathVariable String name) {
        return listTools()
                .filter(tool -> tool.name().equals(name))
                .next()
                .switchIfEmpty(Mono.error(new ResponseStatusException(NOT_FOUND, "Tool not found: " + name)));
    }
}
