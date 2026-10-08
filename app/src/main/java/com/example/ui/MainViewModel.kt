package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.AiTool
import com.example.data.AppDatabase
import com.example.data.ToolRepository
import com.example.data.ToolRunHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    object Dashboard : Screen
    data class CreateTool(val toolId: Long? = null) : Screen
    data class ExecuteTool(val toolId: Long) : Screen
}

sealed interface ApiState {
    object Idle : ApiState
    object Loading : ApiState
    data class Success(val responseText: String) : ApiState
    data class Error(val message: String) : ApiState
}

class MainViewModel(
    application: Application,
    private val repository: ToolRepository
) : AndroidViewModel(application) {

    // Current Navigation Screen
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // All available tools
    val tools: StateFlow<List<AiTool>> = repository.allTools
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current active tool ID for the execute/history screen
    private val _activeToolId = MutableStateFlow<Long?>(null)
    
    // Active tool state
    val activeTool: StateFlow<AiTool?> = _activeToolId.flatMapLatest { id ->
        if (id == null) {
            flowOf<AiTool?>(null)
        } else {
            // Find in current tools
            kotlinx.coroutines.flow.flow {
                val tool = tools.value.find { it.id == id }
                emit(tool)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active tool run history
    val activeToolHistory: StateFlow<List<ToolRunHistory>> = _activeToolId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        } else {
            repository.getHistoryForTool(id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // State of Gemini API execution
    private val _apiState = MutableStateFlow<ApiState>(ApiState.Idle)
    val apiState: StateFlow<ApiState> = _apiState.asStateFlow()

    init {
        // Seed database with default tools if empty on start
        viewModelScope.launch {
            repository.checkAndSeedPresets()
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.ExecuteTool) {
            _activeToolId.value = screen.toolId
            _apiState.value = ApiState.Idle
        } else if (screen is Screen.Dashboard) {
            _activeToolId.value = null
            _apiState.value = ApiState.Idle
        }
    }

    fun createOrUpdateTool(
        id: Long? = null,
        name: String,
        description: String,
        systemPrompt: String,
        inputPlaceholder: String,
        iconName: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            val tool = AiTool(
                id = id ?: 0,
                name = name.trim(),
                description = description.trim(),
                systemPrompt = systemPrompt.trim(),
                inputPlaceholder = inputPlaceholder.trim(),
                iconName = iconName,
                colorHex = colorHex,
                isPreset = false
            )
            if (id == null || id == 0L) {
                repository.insertTool(tool)
            } else {
                repository.updateTool(tool)
            }
            navigateTo(Screen.Dashboard)
        }
    }

    fun deleteTool(tool: AiTool) {
        viewModelScope.launch {
            // Clear history for this tool first
            repository.deleteHistoryForTool(tool.id)
            repository.deleteTool(tool)
            if (_activeToolId.value == tool.id) {
                navigateTo(Screen.Dashboard)
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearHistoryForTool(toolId: Long) {
        viewModelScope.launch {
            repository.deleteHistoryForTool(toolId)
        }
    }

    fun runTool(tool: AiTool, input: String) {
        if (input.isBlank()) {
            _apiState.value = ApiState.Error("Prompt input cannot be empty.")
            return
        }

        _apiState.value = ApiState.Loading

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _apiState.value = ApiState.Error("Gemini API key is not configured. Please add your key in the Secrets Panel in AI Studio.")
                    return@launch
                }

                // Construct system prompt and content request
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = input)))
                    ),
                    generationConfig = GenerationConfig(temperature = 0.7f),
                    systemInstruction = if (tool.systemPrompt.isNotBlank()) {
                        Content(parts = listOf(Part(text = tool.systemPrompt)))
                    } else null
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (generatedText != null) {
                    _apiState.value = ApiState.Success(generatedText)
                    
                    // Save run to history
                    repository.insertHistory(
                        ToolRunHistory(
                            toolId = tool.id,
                            input = input.trim(),
                            output = generatedText
                        )
                    )
                } else {
                    _apiState.value = ApiState.Error("Failed to generate content. Please try again.")
                }
            } catch (e: Exception) {
                _apiState.value = ApiState.Error(e.localizedMessage ?: "An error occurred.")
            }
        }
    }

    class Factory(
        private val application: Application,
        private val repository: ToolRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
