package com.stx.meimo.ui.roledetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stx.meimo.data.model.RoleDetailDto
import com.stx.meimo.data.repository.RoleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoleDetailState(
    val role: RoleDetailDto? = null,
    val isFavourite: Boolean = false,
    val isLoading: Boolean = false,
    val isTogglingFav: Boolean = false,
    val error: String? = null
)

class RoleDetailViewModel(
    private val roleId: Long,
    private val roleRepository: RoleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RoleDetailState())
    val state: StateFlow<RoleDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun toggleFavourite() {
        if (_state.value.isTogglingFav) return
        _state.update { it.copy(isTogglingFav = true) }
        viewModelScope.launch {
            roleRepository.toggleFavourite(roleId)
                .onSuccess { newState ->
                    _state.update { it.copy(isFavourite = newState, isTogglingFav = false) }
                }
                .onFailure {
                    _state.update { it.copy(isTogglingFav = false) }
                }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            roleRepository.getRoleDetail(roleId)
                .onSuccess { detail ->
                    _state.update {
                        it.copy(
                            role = detail,
                            isFavourite = detail.isFavourite == 1,
                            isLoading = false
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "加载失败") }
                }
        }
    }
}
