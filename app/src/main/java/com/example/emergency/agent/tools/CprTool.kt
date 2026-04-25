package com.example.emergency.agent.tools

import com.example.emergency.agent.Tool
import com.example.emergency.agent.ToolResult

/**
 * CPR instruction tool - provides CPR guidance.
 * TODO: Implement full CPR step-by-step guidance with timer/metronome.
 */
class CprTool {
    
    fun getTool(): Tool = Tool(
        name = "cpr_instructions",
        description = "Provides CPR (Cardiopulmonary Resuscitation) step-by-step instructions for adults.",
        execute = ::execute,
    )
    
    private suspend fun execute(params: Map<String, String>): ToolResult {
        // Placeholder implementation
        val instructions = """
            CPR Instructions (Adult):
            
            1. Check responsiveness - tap shoulders and shout "Are you okay?"
            2. Call emergency services (or have someone else call)
            3. Check for breathing (no more than 10 seconds)
            4. If not breathing normally, begin CPR:
            
            CHEST COMPRESSIONS:
            - Place heel of hand on center of chest
            - Place other hand on top, interlock fingers
            - Position shoulders directly over hands
            - Push hard and fast: at least 2 inches deep
            - Rate: 100-120 compressions per minute
            - Give 30 compressions
            
            RESCUE BREATHS:
            - Tilt head back, lift chin
            - Pinch nose shut
            - Give 2 breaths (1 second each)
            - Watch for chest rise
            
            Continue cycles of 30 compressions and 2 breaths until:
            - Emergency help arrives
            - Person starts breathing
            - AED is available
            - You are too exhausted to continue
            
            NOTE: This is a placeholder. Full CPR guidance with timer/metronome will be implemented.
        """.trimIndent()
        
        return ToolResult(
            success = true,
            data = instructions
        )
    }
}
