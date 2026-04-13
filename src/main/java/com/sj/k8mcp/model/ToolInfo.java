package com.sj.k8mcp.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata for a registered MCP tool")
public record ToolInfo(

        @Schema(description = "Unique tool name as registered in the MCP protocol", example = "getPods")
        String name,

        @Schema(description = "Human-readable description of what the tool does and when to use it")
        String description,

        @Schema(description = "JSON Schema defining the tool's input parameters")
        String inputSchema
) {
}
