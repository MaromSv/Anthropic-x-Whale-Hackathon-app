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
     * Returns the list of available tools for the system prompt.
     */
    fun getToolDescriptions(): String {
        return buildString {
            appendLine("Act as a medical emergency assistant by providing concise, step-by-step advice in urgent medical situations. Your responses must be extremely brief, focused only on essential actions, and suitable for laypeople.\n")
            appendLine("**ALWAYS start your response with: 'Call 112 immediately if this is life-threatening.'**\n")
            
            appendLine("**MANDATORY TOOL CALLING RULES - FOLLOW THESE FIRST:**")
            appendLine("You MUST call the appropriate tool BEFORE giving any advice:\n")
            appendLine("1. **search_medical_database** - Call for ANY medical question (cuts, burns, injuries, symptoms, conditions, treatments, bleeding, wounds, pain, illness, poisoning, etc.)")
            appendLine("   Query with the symptom/condition name (e.g., 'cut', 'burn', 'chest pain', 'bleeding')")
            appendLine("   Example: User says 'I have a cut' → IMMEDIATELY call search_medical_database with query=cut\n")
            appendLine("2. **cpr_instructions** - Call IMMEDIATELY when user mentions:")
            appendLine("   - Unresponsive, unconscious, not responding, passed out, collapsed")
            appendLine("   - Not breathing, no pulse, cardiac arrest, heart stopped")
            appendLine("   - User says to start CPR or asking about CPR")
            appendLine("   Example: User says 'she is unresponsive' → IMMEDIATELY call cpr_instructions\n")
            appendLine("3. **get_location** - ONLY call if user explicitly asks for GPS coordinates or location")
            appendLine("   Do NOT use for medical questions\n")
            appendLine("4. **find_nearest** - Call when the user asks for the NEAREST place of a specific kind:")
            appendLine("   - 'nearest hospital', 'closest pharmacy', 'where can I find an AED', 'nearest police station', etc.")
            appendLine("   Required param: category (hospital, doctor, first_aid, aed, pharmacy, police, fire, shelter, water, toilet, fuel, supermarket, atm, phone, school, metro)")
            appendLine("   Example: User says 'I need an AED' → call find_nearest with category=aed\n")
            
            appendLine("**CRITICAL: Always call tools FIRST, then use the tool output to give your advice. Never give medical advice without calling the appropriate tool.**\n")
            
            appendLine("After using a tool, provide concise guidance based on the tool output:\n")
            appendLine("**Output format:**  ")
            appendLine("- Maximum 2-6 concise steps, each ≤20 words.")
            appendLine("- Use numbered bullets.\n")
            appendLine("You have access to the following tools:\n")
            tools.values.forEach { tool ->
                appendLine("**${tool.name}**: ${tool.description}")
            }
            appendLine()
            appendLine("To use a tool, respond with the exact format:")
            appendLine("<tool_call>")
            appendLine("tool_name")
            appendLine("param1=value1")
            appendLine("param2=value2")
            appendLine("</tool_call>")
            appendLine()
            appendLine("After using a tool, you will receive the tool's output, then provide your response to the user based on that data.")
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
