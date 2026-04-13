package com.stx.meimo.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stx.meimo.data.model.RoleCardDto
import com.stx.meimo.data.repository.RoleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CreatorTab(val label: String) {
    MY_ROLES("我的角色"),
    FAVORITES("收藏"),
}

data class CreatorState(
    val selectedTab: CreatorTab = CreatorTab.MY_ROLES,
    val roles: List<RoleCardDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CreatorViewModel(
    private val roleRepository: RoleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreatorState())
    val state: StateFlow<CreatorState> = _state.asStateFlow()

    init {
        load()
    }

    fun selectTab(tab: CreatorTab) {
        if (tab == _state.value.selectedTab) return
        _state.update { it.copy(selectedTab = tab, roles = emptyList()) }
        load()
    }

    fun refresh() = load()

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = when (_state.value.selectedTab) {
                CreatorTab.MY_ROLES -> roleRepository.getMyRoles(page = 1, size = 50)
                CreatorTab.FAVORITES -> roleRepository.getFavorites(page = 1, size = 50)
            }
            result
                .onSuccess { data ->
                    _state.update { it.copy(roles = data.content, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "加载失败") }
                }
        }
    }
}
