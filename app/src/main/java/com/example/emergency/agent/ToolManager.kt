package com.example.emergency.agent

import android.content.Context
import com.example.emergency.agent.tools.CprTool
import com.example.emergency.agent.tools.FindNearestTool
import com.example.emergency.agent.tools.GpsLocationTool
import com.example.emergency.agent.tools.MedicalRagTool

/**
 * Manages available tools and handles tool execution.
 */
class ToolManager(context: Context) {
    
    private val tools: Map<String, Tool>
    
    init {
        val toolList = listOf(
            GpsLocationTool(context).getTool(),
            MedicalRagTool(context).getTool(),
            CprTool().getTool(),
            FindNearestTool(context).getTool(),
        )
        
        tools = toolList.associateBy { it.name }
    }
    
    /**
     * Returns the list of available tools and the tool-call format for the system prompt.
     */
    fun getToolDescriptions(): String {
        return buildString {
            appendLine("Available tools:")
            tools.values.forEach { tool ->
                appendLine("- **${tool.name}**: ${tool.description}")
            }
            appendLine()
            appendLine("Tool call format (must match exactly):")
            appendLine("<tool_call>")
            appendLine("tool_name")
            appendLine("param=value")
            appendLine("</tool_call>")
        }
    }
    
    /**
     * Parses tool calls from the assistant's response.
     * Looks for <tool_call>...</tool_call> blocks.
     */
    fun parseToolCalls(response: String): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()
        val regex = """<tool_call>(.*?)</tool_call>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        regex.findAll(response).forEach { match ->
            val content = match.groupValues[1].trim()
            val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
            
            if (lines.isNotEmpty()) {
                val toolName = lines[0]
                val params = lines.drop(1)
                    .mapNotNull { line ->
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
                    }
                    .toMap()
                
                toolCalls.add(ToolCall(toolName, params))
            }
        }
        
        return toolCalls
    }
    
    /**
     * Executes a tool call and returns the result.
     */
    suspend fun executeTool(toolCall: ToolCall): ToolResult {
        val tool = tools[toolCall.toolName]
            ?: return ToolResult(
                success = false,
                data = "",
                error = "Unknown tool: ${toolCall.toolName}"
            )
        
        return try {
            tool.execute(toolCall.params)
        } catch (e: Exception) {
            ToolResult(
                success = false,
                data = "",
                error = "Tool execution error: ${e.message}"
            )
        }
    }
    
    /**
     * Removes tool call blocks from the response text.
     */
    fun removeToolCallBlocks(response: String): String {
        return response.replace("""<tool_call>.*?</tool_call>""".toRegex(RegexOption.DOT_MATCHES_ALL), "").trim()
    }
}
