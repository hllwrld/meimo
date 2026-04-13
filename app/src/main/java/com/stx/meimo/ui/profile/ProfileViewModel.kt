package com.stx.meimo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stx.meimo.data.model.UserDto
import com.stx.meimo.data.repository.AuthRepository
import com.stx.meimo.data.repository.RewardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val user: UserDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false,
    val isSigningIn: Boolean = false,
    val signInMessage: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val rewardRepository: RewardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    fun logout() {
        authRepository.logout()
        _state.update { it.copy(isLoggedOut = true) }
    }

    fun signIn() {
        val user = _state.value.user ?: return
        if (user.todayIsSign) return
        _state.update { it.copy(isSigningIn = true, signInMessage = null) }
        viewModelScope.launch {
            rewardRepository.signIn()
                .onSuccess {
                    _state.update {
                        it.copy(
                            isSigningIn = false,
                            signInMessage = "签到成功！",
                            user = it.user?.copy(todayIsSign = true)
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSigningIn = false, signInMessage = e.message ?: "签到失败") }
                }
        }
    }

    fun clearSignInMessage() {
        _state.update { it.copy(signInMessage = null) }
    }

    private fun load() {
        if (!authRepository.isLoggedIn) {
            _state.update { it.copy(error = "未登录") }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.getUserInfo()
                .onSuccess { user ->
                    _state.update { it.copy(user = user, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "加载失败") }
                }
        }
    }
}
