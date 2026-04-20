package com.stx.meimo.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.stx.meimo.data.model.ConversationDto
import com.stx.meimo.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatListState(
    val conversations: List<ConversationDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatListViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            Log.d("ChatListVM", "Loading conversations...")
            chatRepository.getAllConversations()
                .onSuccess { conversations ->
                    Log.d("ChatListVM", "Loaded ${conversations.size} conversations")
                    _state.update { it.copy(conversations = conversations, isLoading = false) }
                }
                .onFailure { e ->
                    Log.e("ChatListVM", "Failed to load conversations", e)
                    _state.update { it.copy(isLoading = false, error = e.message ?: "加载失败") }
                }
        }
    }
}
