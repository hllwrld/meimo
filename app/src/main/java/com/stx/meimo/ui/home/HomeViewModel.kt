package com.stx.meimo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stx.meimo.data.model.CategoryDto
import com.stx.meimo.data.model.RoleCardDto
import com.stx.meimo.data.repository.RoleRepository
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeTab(val label: String) {
    RANKING("榜单"),
    RECOMMEND("推荐"),
    HOT("最热"),
    NEW("最新")
}

enum class RankingPeriod(val label: String, val dayNum: Int) {
    DAILY("日榜", 1),
    WEEKLY("周榜", 7),
    MONTHLY("月榜", 30)
}

enum class CategoryFilterMode(val label: String) {
    OR("或(OR)"),
    AND("与(AND)")
}

data class HomeState(
    val categories: List<CategoryDto> = emptyList(),
    val selectedCategoryIds: Set<Int> = emptySet(),
    val categoryFilterMode: CategoryFilterMode = CategoryFilterMode.OR,
    val selectedTab: HomeTab = HomeTab.RANKING,
    val rankingPeriod: RankingPeriod = RankingPeriod.DAILY,
    val roles: List<RoleCardDto> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val page: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null
)

class HomeViewModel(
    private val roleRepository: RoleRepository
) : ViewModel() {

    companion object {
        private const val TAG = "HomeVM"
        private const val PAGE_SIZE = 20
    }

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadCategories()
        loadRoles(refresh = true)
    }

    fun toggleCategory(categoryId: Int) {
        _state.update {
            val newIds = if (categoryId in it.selectedCategoryIds) {
                it.selectedCategoryIds - categoryId
            } else {
                it.selectedCategoryIds + categoryId
            }
            it.copy(selectedCategoryIds = newIds)
        }
        loadRoles(refresh = true)
    }

    fun clearCategories() {
        _state.update { it.copy(selectedCategoryIds = emptySet()) }
        loadRoles(refresh = true)
    }

    fun toggleCategoryFilterMode() {
        _state.update {
            val newMode = if (it.categoryFilterMode == CategoryFilterMode.OR) CategoryFilterMode.AND else CategoryFilterMode.OR
            it.copy(categoryFilterMode = newMode)
        }
        if (_state.value.selectedCategoryIds.size > 1) {
            loadRoles(refresh = true)
        }
    }

    fun selectTab(tab: HomeTab) {
        if (_state.value.selectedTab == tab) return
        _state.update { it.copy(selectedTab = tab) }
        loadRoles(refresh = true)
    }

    fun selectRankingPeriod(period: RankingPeriod) {
        if (_state.value.rankingPeriod == period) return
        _state.update { it.copy(rankingPeriod = period) }
        loadRoles(refresh = true)
    }

    fun refresh() {
        loadRoles(refresh = true)
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore) return
        loadRoles(refresh = false)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            roleRepository.getCategories().onSuccess { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
    }

    private fun loadRoles(refresh: Boolean) {
        val current = _state.value
        val startPage = if (refresh) 1 else current.page
        val selectedIds = current.selectedCategoryIds
        val filterMode = current.categoryFilterMode
        val needsClientFilter = selectedIds.size > 1

        // API only reliably supports single categoryId; multi-select uses client-side filtering
        val apiCategoryIds = if (selectedIds.size == 1) selectedIds.toList() else emptyList()

        Log.d(TAG, "loadRoles: refresh=$refresh, tab=${current.selectedTab}, startPage=$startPage, hasMore=${current.hasMore}")

        _state.update {
            if (refresh) it.copy(isLoading = true, error = null)
            else it.copy(isLoadingMore = true)
        }

        viewModelScope.launch {
            val allFiltered = mutableListOf<RoleCardDto>()
            var currentPage = startPage
            var apiHasMore = true
            // When multi-select filtering, try up to 5 pages to find matching results
            val maxPages = if (needsClientFilter) 5 else 1

            for (attempt in 1..maxPages) {
                val result = when (current.selectedTab) {
                    HomeTab.RANKING -> roleRepository.getHotRoles(
                        page = currentPage, size = PAGE_SIZE, categoryIds = apiCategoryIds, dayNum = current.rankingPeriod.dayNum
                    )
                    HomeTab.RECOMMEND -> roleRepository.getRecommendedRoles(
                        page = currentPage, size = PAGE_SIZE, categoryIds = apiCategoryIds
                    )
                    HomeTab.HOT -> roleRepository.getHotRoles(
                        page = currentPage, size = PAGE_SIZE, categoryIds = apiCategoryIds
                    )
                    HomeTab.NEW -> roleRepository.getRoleList(
                        page = currentPage, size = PAGE_SIZE, categoryIds = apiCategoryIds
                    )
                }

                val pagedData = result.getOrElse { e ->
                    Log.e(TAG, "loadRoles error: page=$currentPage, error=${e.message}")
                    _state.update {
                        it.copy(isLoading = false, isLoadingMore = false, error = e.message ?: "未知错误")
                    }
                    return@launch
                }

                val contentSize = pagedData.content.size
                // hasMore based on whether API returned a full page
                apiHasMore = contentSize >= PAGE_SIZE
                Log.d(TAG, "loadRoles: page=$currentPage, contentSize=$contentSize, total=${pagedData.total}, apiHasMore=$apiHasMore")

                var content = pagedData.content

                if (needsClientFilter) {
                    content = if (filterMode == CategoryFilterMode.AND) {
                        content.filter { role -> role.categoryIds?.containsAll(selectedIds) == true }
                    } else {
                        content.filter { role -> role.categoryIds?.any { it in selectedIds } == true }
                    }
                }

                allFiltered.addAll(content)
                currentPage++

                // Stop if: API exhausted, or we found some results, or single-select (no client filter)
                if (!apiHasMore || allFiltered.isNotEmpty() || !needsClientFilter) break
            }

            _state.update { state ->
                val existingIds = if (refresh) emptySet() else state.roles.map { it.id }.toSet()
                val deduped = allFiltered.filter { it.id !in existingIds }
                val newRoles = if (refresh) deduped else state.roles + deduped
                // hasMore: API had a full page of data, and we got at least some new items
                // Also keep hasMore=true if API had full page but all were dupes (edge case retry)
                val hasMore = apiHasMore
                Log.d(TAG, "loadRoles done: existingCount=${state.roles.size}, fetched=${allFiltered.size}, deduped=${deduped.size}, newTotal=${newRoles.size}, hasMore=$hasMore, nextPage=$currentPage")
                state.copy(
                    roles = newRoles,
                    page = currentPage,
                    hasMore = hasMore,
                    isLoading = false,
                    isLoadingMore = false,
                    error = null
                )
            }
        }
    }
}
