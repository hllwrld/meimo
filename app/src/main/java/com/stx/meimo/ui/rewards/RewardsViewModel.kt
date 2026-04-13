package com.stx.meimo.ui.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stx.meimo.data.model.UserDto
import com.stx.meimo.data.remote.TaskDto
import com.stx.meimo.data.repository.AuthRepository
import com.stx.meimo.data.repository.RewardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RewardsState(
    val user: UserDto? = null,
    val tasks: List<TaskDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSigningIn: Boolean = false,
    val signedIn: Boolean = false,
    val error: String? = null,
    val signInMessage: String? = null
)

class RewardsViewModel(
    private val rewardRepository: RewardRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RewardsState())
    val state: StateFlow<RewardsState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    fun signIn() {
        if (_state.value.isSigningIn || _state.value.signedIn) return
        _state.update { it.copy(isSigningIn = true) }
        viewModelScope.launch {
            rewardRepository.signIn()
                .onSuccess {
                    _state.update {
                        it.copy(
                            isSigningIn = false,
                            signedIn = true,
                            signInMessage = "签到成功"
                        )
                    }
                    loadUserInfo()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSigningIn = false,
                            signInMessage = e.message ?: "签到失败"
                        )
                    }
                }
        }
    }

    fun clearSignInMessage() {
        _state.update { it.copy(signInMessage = null) }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            loadUserInfo()
            rewardRepository.getTaskList()
                .onSuccess { tasks ->
                    _state.update { it.copy(tasks = tasks, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private suspend fun loadUserInfo() {
        authRepository.getUserInfo()
            .onSuccess { user ->
                _state.update {
                    it.copy(
                        user = user,
                        signedIn = user.todayIsSign
                    )
                }
            }
    }
}
