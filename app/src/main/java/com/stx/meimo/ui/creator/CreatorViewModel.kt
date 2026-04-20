package com.stx.meimo.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stx.meimo.data.model.CategoryDto
import com.stx.meimo.data.model.RoleCardDto
import com.stx.meimo.data.repository.RoleRepository
import com.stx.meimo.ui.home.CategoryFilterMode
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
    val filteredRoles: List<RoleCardDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val selectedCategoryIds: Set<Int> = emptySet(),
    val categoryFilterMode: CategoryFilterMode = CategoryFilterMode.OR,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CreatorViewModel(
    private val roleRepository: RoleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreatorState())
    val state: StateFlow<CreatorState> = _state.asStateFlow()

    init {
        loadCategories()
        load()
    }

    fun selectTab(tab: CreatorTab) {
        if (tab == _state.value.selectedTab) return
        _state.update {
            it.copy(
                selectedTab = tab,
                roles = emptyList(),
                filteredRoles = emptyList(),
                selectedCategoryIds = if (tab != CreatorTab.FAVORITES) emptySet() else it.selectedCategoryIds
            )
        }
        load()
    }

    fun refresh() = load()

    fun toggleCategory(categoryId: Int) {
        _state.update {
            val newIds = if (categoryId in it.selectedCategoryIds)
                it.selectedCategoryIds - categoryId
            else
                it.selectedCategoryIds + categoryId
            it.copy(selectedCategoryIds = newIds)
        }
        applyFilter()
    }

    fun clearCategories() {
        _state.update { it.copy(selectedCategoryIds = emptySet()) }
        applyFilter()
    }

    fun toggleCategoryFilterMode() {
        _state.update {
            val newMode = if (it.categoryFilterMode == CategoryFilterMode.OR)
                CategoryFilterMode.AND else CategoryFilterMode.OR
            it.copy(categoryFilterMode = newMode)
        }
        applyFilter()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            roleRepository.getCategories().onSuccess { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
    }

    private fun applyFilter() {
        _state.update { state ->
            val selectedIds = state.selectedCategoryIds
            val filtered = if (selectedIds.isEmpty()) {
                state.roles
            } else if (state.categoryFilterMode == CategoryFilterMode.AND) {
                state.roles.filter { role ->
                    role.categoryIds?.containsAll(selectedIds) == true
                }
            } else {
                state.roles.filter { role ->
                    role.categoryIds?.any { it in selectedIds } == true
                }
            }
            state.copy(filteredRoles = filtered)
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = when (_state.value.selectedTab) {
                CreatorTab.MY_ROLES -> roleRepository.getMyRoles(page = 1, size = 50)
                CreatorTab.FAVORITES -> roleRepository.getFavorites(page = 1, size = 200)
            }
            result
                .onSuccess { data ->
                    _state.update { it.copy(roles = data.content, isLoading = false) }
                    applyFilter()
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "加载失败") }
                }
        }
    }
}
