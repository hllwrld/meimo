package com.stx.meimo.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stx.meimo.data.model.CategoryDto
import com.stx.meimo.ui.component.ErrorState
import com.stx.meimo.ui.component.LoadingIndicator
import com.stx.meimo.ui.component.LocalHideImages
import com.stx.meimo.ui.component.RoleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onRoleClick: (Long) -> Unit,
    onProfile: () -> Unit = {},
    onSearch: () -> Unit = {},
    onDebugWebView: () -> Unit = {},
    onToggleHideImages: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    var showCategoryDialog by remember { mutableStateOf(false) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 6 && !state.isLoadingMore && state.hasMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

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
                title = {
                    Text(
                        "Meimo",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.clickable { onDebugWebView() }
                    )
                },
                actions = {
                    val hideImages = LocalHideImages.current
                    IconButton(onClick = onToggleHideImages, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (hideImages) Icons.Default.ImageNotSupported else Icons.Default.Image,
                            contentDescription = "屏蔽图片",
                            modifier = Modifier.size(20.dp),
                            tint = if (hideImages) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showCategoryDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.FilterList, contentDescription = "分类", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onSearch, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Search, contentDescription = "搜索", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onProfile, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "个人资料", modifier = Modifier.size(20.dp))
                    }
                },
                expandedHeight = 36.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        if (state.isLoading && state.roles.isEmpty()) {
            LoadingIndicator(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        if (state.error != null && state.roles.isEmpty()) {
            ErrorState(
                message = state.error!!,
                onRetry = { viewModel.refresh() },
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading && state.roles.isNotEmpty(),
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tab row
                item(span = { GridItemSpan(2) }) {
                    ScrollableTabRow(
                        selectedTabIndex = HomeTab.entries.indexOf(state.selectedTab),
                        edgePadding = 0.dp
                    ) {
                        HomeTab.entries.forEach { tab ->
                            Tab(
                                selected = state.selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                text = { Text(tab.label) }
                            )
                        }
                    }
                }

                // Ranking period chips
                if (state.selectedTab == HomeTab.RANKING) {
                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RankingPeriod.entries.forEach { period ->
                                FilterChip(
                                    selected = state.rankingPeriod == period,
                                    onClick = { viewModel.selectRankingPeriod(period) },
                                    label = { Text(period.label) }
                                )
                            }
                        }
                    }
                }

                // Selected categories indicator
                if (state.selectedCategoryIds.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
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
                                    label = { Text("$name ✕", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }
                    }
                }

                // Role cards
                items(state.roles, key = { it.id }) { role ->
                    RoleCard(
                        role = role,
                        onClick = { onRoleClick(role.id) },
                        categories = state.categories
                    )
                }

                if (state.isLoadingMore) {
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // Manual "load more" for multi-select filtering
                if (state.selectedCategoryIds.size > 1 && !state.isLoadingMore) {
                    item(span = { GridItemSpan(2) }) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (state.roles.isEmpty()) {
                                Text(
                                    text = "未找到匹配角色",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            androidx.compose.material3.OutlinedButton(
                                onClick = { viewModel.loadMore() },
                                enabled = state.hasMore
                            ) {
                                Text(if (state.hasMore) "继续加载" else "已加载全部")
                            }
                        }
                    }
                }
            }
        }
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
                // OR/AND toggle
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
