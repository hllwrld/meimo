package com.stx.meimo.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stx.meimo.data.model.RoleCardDto
import com.stx.meimo.data.repository.RoleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val results: List<RoleCardDto> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val page: Int = 1,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false
)

class SearchViewModel(
    private val roleRepository: RoleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(results = emptyList(), hasSearched = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // debounce
            search(query)
        }
    }

    fun searchNow() {
        val query = _state.value.query.trim()
        if (query.isBlank()) return
        searchJob?.cancel()
        viewModelScope.launch { search(query) }
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore || current.query.isBlank()) return

        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            val nextPage = current.page + 1
            roleRepository.searchRoles(current.query, page = nextPage)
                .onSuccess { data ->
                    _state.update {
                        it.copy(
                            results = it.results + data.content,
                            page = nextPage,
                            hasMore = data.content.size >= 20,
                            isLoadingMore = false
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    private suspend fun search(query: String) {
        _state.update { it.copy(isSearching = true) }
        roleRepository.searchRoles(query, page = 1)
            .onSuccess { data ->
                _state.update {
                    it.copy(
                        results = data.content,
                        isSearching = false,
                        hasSearched = true,
                        page = 1,
                        hasMore = data.content.size >= 20
                    )
                }
            }
            .onFailure {
                _state.update { it.copy(isSearching = false, hasSearched = true) }
            }
    }
}
