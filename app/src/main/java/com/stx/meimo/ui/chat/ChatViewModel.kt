package com.stx.meimo.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stx.meimo.data.model.MessageDto
import com.stx.meimo.data.model.ModelCategoryDto
import com.stx.meimo.data.model.ModelDto
import com.stx.meimo.data.model.RoleDetailDto
import com.stx.meimo.data.remote.InstructionDto
import com.stx.meimo.data.repository.ChatRepository
import com.stx.meimo.data.repository.ModelRepository
import com.stx.meimo.data.repository.RoleRepository
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatState(
    val messages: List<MessageDto> = emptyList(),
    val models: List<ModelDto> = emptyList(),
    val modelCategories: List<ModelCategoryDto> = emptyList(),
    val selectedModel: ModelDto? = null,
    val selectedCategoryId: Int? = null,
    val roleDetail: RoleDetailDto? = null,
    val instructions: List<InstructionDto> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val showModelSelector: Boolean = false,
    val showChatMenu: Boolean = false,
    val showPersonaDialog: Boolean = false,
    val userPersona: String = "",
    val page: Int = 1,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false
)

class ChatViewModel(
    private val roleId: Long,
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository,
    private val roleRepository: RoleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        loadRoleDetail()
        loadMessages()
        loadModels()
        loadModelCategories()
        loadInstructions()
    }

    fun updateInput(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun toggleModelSelector() {
        _state.update { it.copy(showModelSelector = !it.showModelSelector) }
    }

    fun selectModel(model: ModelDto) {
        _state.update { it.copy(selectedModel = model, showModelSelector = false) }
    }

    fun selectModelCategory(categoryId: Int?) {
        _state.update { it.copy(selectedCategoryId = categoryId) }
        loadModels(categoryId)
    }

    fun dismissModelSelector() {
        _state.update { it.copy(showModelSelector = false) }
    }

    fun toggleChatMenu() {
        _state.update { it.copy(showChatMenu = !it.showChatMenu) }
    }

    fun dismissChatMenu() {
        _state.update { it.copy(showChatMenu = false) }
    }

    fun useInstruction(instruction: InstructionDto) {
        val content = instruction.content
        if (content.isNullOrBlank()) {
            // "清空输入框" instruction has null content
            _state.update { it.copy(inputText = "") }
            return
        }
        _state.update { it.copy(inputText = content) }
    }

    fun startNewChat() {
        _state.update { it.copy(showChatMenu = false) }
        Log.d("ChatVM", "startNewChat: roleId=$roleId")
        viewModelScope.launch {
            chatRepository.createConversation(roleId)
                .onSuccess { conv ->
                    Log.d("ChatVM", "New conversation created: id=${conv.id}")
                    val before = _state.value.messages.size
                    _state.update {
                        it.copy(
                            messages = emptyList(),
                            page = 1,
                            hasMore = false,
                            error = null
                        )
                    }
                    val after = _state.value.messages.size
                    Log.d("ChatVM", "Messages cleared: $before -> $after")
                    maybeInsertPrologue()
                    Log.d("ChatVM", "After prologue: ${_state.value.messages.size} messages")
                    _state.update { it.copy(error = "已创建新对话") }
                }
                .onFailure { e ->
                    Log.e("ChatVM", "Create conversation failed: ${e.message}", e)
                    _state.update { it.copy(error = "创建新对话失败: ${e.message}") }
                }
        }
    }

    fun showPersonaDialog() {
        _state.update { it.copy(showPersonaDialog = true, showChatMenu = false) }
    }

    fun dismissPersonaDialog() {
        _state.update { it.copy(showPersonaDialog = false) }
    }

    fun updatePersona(persona: String) {
        _state.update { it.copy(userPersona = persona, showPersonaDialog = false) }
    }

    fun sendMessage() {
        val current = _state.value
        val content = current.inputText.trim()
        if (content.isBlank() || current.isSending) return

        val model = current.selectedModel
        if (model == null) {
            _state.update { it.copy(error = "请先选择一个模型") }
            return
        }

        val userMsg = MessageDto(
            id = System.currentTimeMillis(),
            roleId = roleId,
            direction = 1,
            content = content,
            model = null,
            createTime = null
        )

        _state.update {
            it.copy(
                messages = listOf(userMsg) + it.messages,
                inputText = "",
                isSending = true,
                error = null
            )
        }

        viewModelScope.launch {
            chatRepository.sendMessage(roleId, content, model.id)
                .onSuccess { aiMessage ->
                    Log.d("ChatVM", "AI reply: ${aiMessage.content?.take(80)}")
                    _state.update {
                        it.copy(
                            messages = listOf(aiMessage) + it.messages,
                            isSending = false
                        )
                    }
                }
                .onFailure { e ->
                    Log.e("ChatVM", "Send failed: ${e.message}")
                    _state.update {
                        it.copy(
                            isSending = false,
                            error = e.message ?: "发送失败"
                        )
                    }
                }
        }
    }

    fun retry() {
        _state.update { it.copy(error = null) }
        loadMessages()
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore) return

        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            val nextPage = current.page + 1
            chatRepository.getMessageHistory(roleId, page = nextPage)
                .onSuccess { data ->
                    _state.update {
                        it.copy(
                            messages = it.messages + data.content,
                            page = nextPage,
                            hasMore = data.content.size >= 50,
                            isLoadingMore = false
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun loadRoleDetail() {
        viewModelScope.launch {
            roleRepository.getRoleDetail(roleId)
                .onSuccess { detail ->
                    _state.update { it.copy(roleDetail = detail) }
                    maybeInsertPrologue()
                }
                .onFailure { e ->
                    Log.w("ChatVM", "Failed to load role detail: ${e.message}")
                }
        }
    }

    private fun loadMessages() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            chatRepository.getMessageHistory(roleId, page = 1)
                .onSuccess { data ->
                    _state.update {
                        it.copy(
                            messages = data.content,
                            isLoading = false,
                            page = 1,
                            hasMore = data.content.size >= 50
                        )
                    }
                    maybeInsertPrologue()
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "加载失败") }
                }
        }
    }

    private fun maybeInsertPrologue() {
        val current = _state.value
        val detail = current.roleDetail ?: return
        // Only insert prologue if message list is empty (no real history)
        if (current.messages.isNotEmpty()) return
        if (current.isLoading) return

        val prologueContent = detail.beginningZh
            ?: detail.beginning
            ?: detail.prologue?.firstOrNull()
            ?: return

        val prologueMsg = MessageDto(
            id = -1,
            roleId = roleId,
            direction = 2,
            content = prologueContent,
            model = null,
            createTime = null
        )
        _state.update { it.copy(messages = listOf(prologueMsg)) }
    }

    private fun loadModels(categoryId: Int? = null) {
        viewModelScope.launch {
            Log.d("ChatVM", "Loading models (categoryId=$categoryId)...")
            modelRepository.getModels(categoryId)
                .onSuccess { models ->
                    Log.d("ChatVM", "Models loaded: ${models.size} models")
                    _state.update {
                        it.copy(
                            models = models,
                            selectedModel = it.selectedModel ?: models.firstOrNull()
                        )
                    }
                }
                .onFailure { e ->
                    Log.e("ChatVM", "Failed to load models: ${e.message}", e)
                    _state.update { it.copy(error = "加载模型失败: ${e.message}") }
                }
        }
    }

    private fun loadModelCategories() {
        viewModelScope.launch {
            modelRepository.getModelCategories()
                .onSuccess { categories ->
                    _state.update { it.copy(modelCategories = categories) }
                }
                .onFailure { e ->
                    Log.w("ChatVM", "Failed to load model categories: ${e.message}")
                }
        }
    }

    private fun loadInstructions() {
        viewModelScope.launch {
            chatRepository.getInstructions()
                .onSuccess { instructions ->
                    _state.update { it.copy(instructions = instructions) }
                }
                .onFailure { e ->
                    Log.w("ChatVM", "Failed to load instructions: ${e.message}")
                }
        }
    }
}
