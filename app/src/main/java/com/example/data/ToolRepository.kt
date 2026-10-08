package com.example.data

import kotlinx.coroutines.flow.Flow

class ToolRepository(
    private val aiToolDao: AiToolDao,
    private val toolRunHistoryDao: ToolRunHistoryDao
) {
    val allTools: Flow<List<AiTool>> = aiToolDao.getAllTools()

    suspend fun insertTool(tool: AiTool): Long {
        return aiToolDao.insertTool(tool)
    }

    suspend fun updateTool(tool: AiTool) {
        aiToolDao.updateTool(tool)
    }

    suspend fun deleteTool(tool: AiTool) {
        aiToolDao.deleteTool(tool)
    }

    fun getHistoryForTool(toolId: Long): Flow<List<ToolRunHistory>> {
        return toolRunHistoryDao.getHistoryForTool(toolId)
    }

    suspend fun insertHistory(history: ToolRunHistory): Long {
        return toolRunHistoryDao.insertHistory(history)
    }

    suspend fun deleteHistoryForTool(toolId: Long) {
        toolRunHistoryDao.deleteHistoryForTool(toolId)
    }

    suspend fun deleteHistoryItem(id: Long) {
        toolRunHistoryDao.deleteHistoryItem(id)
    }

    suspend fun checkAndSeedPresets() {
        if (aiToolDao.getToolsCount() == 0) {
            val presets = listOf(
                AiTool(
                    name = "Email Professionalizer",
                    description = "Transforms rough bullet points into polite, professional emails.",
                    systemPrompt = "You are an expert executive assistant. Convert the user's rough bullet points or draft into a highly polished, professional email. Keep it concise, courteous, and clear. Maintain a business-professional tone.",
                    inputPlaceholder = "Write your rough bullet points here (e.g., tell boss I'm sick and won't make meeting but will send slides)...",
                    iconName = "email",
                    colorHex = "#4F46E5",
                    isPreset = true
                ),
                AiTool(
                    name = "Code Explainer",
                    description = "Explains code snippets step-by-step for easy learning.",
                    systemPrompt = "You are a friendly, expert software engineer tutor. Explain the following code snippet step-by-step. Break it down so that a beginner can understand it. Highlight the main logic and what each part does.",
                    inputPlaceholder = "Paste your code snippet here...",
                    iconName = "code",
                    colorHex = "#10B981",
                    isPreset = true
                ),
                AiTool(
                    name = "Summary Generator",
                    description = "Condenses long articles or text into key bullet points.",
                    systemPrompt = "You are an expert reader and researcher. Summarize the following text into 3-5 concise, high-impact bullet points, followed by a 1-sentence key takeaway.",
                    inputPlaceholder = "Paste the long text or article content here...",
                    iconName = "summarize",
                    colorHex = "#F59E0B",
                    isPreset = true
                ),
                AiTool(
                    name = "Creative Storyteller",
                    description = "Generates short creative stories and narrative plots from simple ideas.",
                    systemPrompt = "You are an award-winning creative author. Take the user's brief story prompt, genre, or character idea, and expand it into a short, engaging, and atmospheric story with rich sensory details. Keep it under 400 words.",
                    inputPlaceholder = "Type your story idea, genre, or characters...",
                    iconName = "story",
                    colorHex = "#EC4899",
                    isPreset = true
                )
            )
            for (preset in presets) {
                aiToolDao.insertTool(preset)
            }
        }
    }
}
