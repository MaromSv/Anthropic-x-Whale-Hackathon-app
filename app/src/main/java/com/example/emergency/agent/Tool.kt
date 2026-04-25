package com.example.emergency.agent

/**
 * Represents a tool that the agent can invoke.
 */
data class Tool(
    val name: String,
    val description: String,
    val execute: suspend (params: Map<String, String>) -> ToolResult,
)

/**
 * Result from tool execution.
 */
data class ToolResult(
    val success: Boolean,
    val data: String,
    val error: String? = null,
)

/**
 * Tool call detected in the assistant's response.
 */
data class ToolCall(
    val toolName: String,
    val params: Map<String, String> = emptyMap(),
)
