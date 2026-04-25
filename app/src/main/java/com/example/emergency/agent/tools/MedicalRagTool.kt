package com.example.emergency.agent.tools

import android.content.Context
import com.example.emergency.agent.Tool
import com.example.emergency.agent.ToolResult
import org.json.JSONObject
import java.io.File

/**
 * Medical RAG tool - searches through medical database for relevant information.
 */
class MedicalRagTool(private val context: Context) {
    
    private var medicalDocuments: List<MedicalDocument> = emptyList()
    
    data class MedicalDocument(
        val title: String,
        val category: String,
        val content: String,
        val tags: List<String>,
        val priority: String,
    )
    
    fun getTool(): Tool = Tool(
        name = "search_medical_database",
        description = "Retrieves medical protocols for symptoms, wounds, injuries, or treatments. Required param: query (e.g., 'tourniquet', 'burn', 'chest pain'). Use for any medical question, including image-based wound assessment.",
        execute = ::execute,
    )

    init {
        loadMedicalDatabase()
    }
    
    private fun loadMedicalDatabase() {
        try {
            // Load from assets
            val jsonContent = context.assets.open("core_medical.json").bufferedReader().use { it.readText() }
            
            val jsonObject = JSONObject(jsonContent)
            val documentsArray = jsonObject.getJSONArray("documents")
            
            medicalDocuments = (0 until documentsArray.length()).map { i ->
                val doc = documentsArray.getJSONObject(i)
                val tagsArray = doc.optJSONArray("tags")
                val tags = if (tagsArray != null) {
                    (0 until tagsArray.length()).map { j -> tagsArray.getString(j) }
                } else {
                    emptyList()
                }
                
                MedicalDocument(
                    title = doc.getString("title"),
                    category = doc.getString("category"),
                    content = doc.getString("content"),
                    tags = tags,
                    priority = doc.optString("priority", "normal")
                )
            }
        } catch (e: Exception) {
            // Silently fail - tool will return no results
            medicalDocuments = emptyList()
        }
    }
    
    private suspend fun execute(params: Map<String, String>): ToolResult {
        val query = params["query"]?.lowercase() ?: return ToolResult(
            success = false,
            data = "",
            error = "No query provided"
        )
        
        if (medicalDocuments.isEmpty()) {
            return ToolResult(
                success = false,
                data = "",
                error = "Medical database not loaded"
            )
        }
        
        // Simple keyword-based search
        val results = medicalDocuments
            .map { doc ->
                val titleMatch = if (doc.title.lowercase().contains(query)) 10 else 0
                val categoryMatch = if (doc.category.lowercase().contains(query)) 5 else 0
                val contentMatch = countOccurrences(doc.content.lowercase(), query)
                val tagMatch = if (doc.tags.any { it.lowercase().contains(query) }) 8 else 0
                val priorityBoost = if (doc.priority == "high") 3 else 0
                
                val score = titleMatch + categoryMatch + contentMatch + tagMatch + priorityBoost
                Pair(doc, score)
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(3)
        
        if (results.isEmpty()) {
            return ToolResult(
                success = true,
                data = "No relevant medical information found for: $query"
            )
        }
        
        val resultText = buildString {
            appendLine("Medical database search results for '$query':\n")
            results.forEachIndexed { index, (doc, score) ->
                appendLine("${index + 1}. ${doc.title} (${doc.category})")
                
                // Extract relevant excerpt
                val excerpt = extractRelevantExcerpt(doc.content, query)
                appendLine(excerpt)
                appendLine()
            }
        }
        
        return ToolResult(
            success = true,
            data = resultText.trim()
        )
    }
    
    private fun countOccurrences(text: String, query: String): Int {
        var count = 0
        var index = 0
        while (text.indexOf(query, index).also { index = it } != -1) {
            count++
            index += query.length
        }
        return count
    }
    
    private fun extractRelevantExcerpt(content: String, query: String, maxLength: Int = 400): String {
        val lowerContent = content.lowercase()
        val queryIndex = lowerContent.indexOf(query.lowercase())
        
        if (queryIndex == -1) {
            // No direct match, return beginning
            return content.take(maxLength) + if (content.length > maxLength) "..." else ""
        }
        
        // Extract context around the query
        val start = maxOf(0, queryIndex - 150)
        val end = minOf(content.length, queryIndex + query.length + 250)
        
        var excerpt = content.substring(start, end)
        if (start > 0) excerpt = "..." + excerpt
        if (end < content.length) excerpt += "..."
        
        return excerpt.trim()
    }
}
