package com.stx.meimo.ui.creator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stx.meimo.data.model.CategoryDto
import com.stx.meimo.ui.component.ErrorState
import com.stx.meimo.ui.component.LoadingIndicator
import com.stx.meimo.ui.component.RoleCard
import com.stx.meimo.ui.home.CategoryFilterMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorScreen(
    viewModel: CreatorViewModel,
    onRoleClick: (Long) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCategoryDialog by remember { mutableStateOf(false) }

    if (showCategoryDialog) {
        CategoryPickerDialog(
            categories = state.categories,
            selectedIds = state.selectedCategoryIds,
            filterMode = state.categoryFilterMode,
            onToggle = { viewModel.toggleCategory(it) },
            onClearAll = { viewModel.clearCategories() },
            onToggleMode = { viewModel.toggleCategoryFilterMode() },
            onDismiss = { showCategoryDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创作") },
                actions = {
                    if (state.selectedTab == CreatorTab.FAVORITES) {
                        IconButton(onClick = { showCategoryDialog = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.FilterList, contentDescription = "分类筛选", modifier = Modifier.size(20.dp))
                        }
                    }
                },
                expandedHeight = 36.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = CreatorTab.entries.indexOf(state.selectedTab)
            ) {
                CreatorTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.label) }
                    )
                }
            }

            // Selected categories indicator
            if (state.selectedTab == CreatorTab.FAVORITES && state.selectedCategoryIds.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (state.categoryFilterMode == CategoryFilterMode.AND) "AND" else "OR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    state.selectedCategoryIds.forEach { id ->
                        val name = state.categories.find { it.id == id }?.name ?: ""
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.toggleCategory(id) },
                            label = { Text("$name \u2715", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }
            }

            when {
                state.isLoading && state.filteredRoles.isEmpty() ->
                    LoadingIndicator(modifier = Modifier.weight(1f))

                state.error != null && state.filteredRoles.isEmpty() ->
                    ErrorState(
                        message = state.error!!,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.weight(1f)
                    )

                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.isLoading,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.filteredRoles.isEmpty()) {
                            EmptyCreator(state.selectedTab, hasActiveFilter = state.selectedCategoryIds.isNotEmpty())
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                items(state.filteredRoles, key = { it.id }) { role ->
                                    RoleCard(
                                        role = role,
                                        onClick = { onRoleClick(role.id) },
                                        modifier = Modifier.padding(4.dp),
                                        categories = state.categories
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCreator(tab: CreatorTab, hasActiveFilter: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                tab == CreatorTab.FAVORITES && hasActiveFilter -> "没有匹配的收藏角色"
                tab == CreatorTab.FAVORITES -> "还没有收藏角色"
                tab == CreatorTab.MY_ROLES -> "还没有创建角色"
                else -> ""
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPickerDialog(
    categories: List<CategoryDto>,
    selectedIds: Set<Int>,
    filterMode: CategoryFilterMode,
    onToggle: (Int) -> Unit,
    onClearAll: () -> Unit,
    onToggleMode: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("选择分类")
                Spacer(modifier = Modifier.width(12.dp))
                FilterChip(
                    selected = true,
                    onClick = onToggleMode,
                    label = { Text(filterMode.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
            }
        },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { category ->
                    if (category.id != null) {
                        FilterChip(
                            selected = category.id in selectedIds,
                            onClick = { onToggle(category.id) },
                            label = { Text(category.name, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        dismissButton = {
            if (selectedIds.isNotEmpty()) {
                TextButton(onClick = onClearAll) { Text("清除全部") }
            }
        }
    )
}
