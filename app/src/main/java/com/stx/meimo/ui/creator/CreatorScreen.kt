package com.stx.meimo.ui.creator

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stx.meimo.ui.component.ErrorState
import com.stx.meimo.ui.component.LoadingIndicator
import com.stx.meimo.ui.component.RoleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorScreen(
    viewModel: CreatorViewModel,
    onRoleClick: (Long) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("创作") }, expandedHeight = 36.dp, windowInsets = WindowInsets(0, 0, 0, 0))
        }
    ) { padding ->
        androidx.compose.foundation.layout.Column(
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

            when {
                state.isLoading && state.roles.isEmpty() ->
                    LoadingIndicator(modifier = Modifier.weight(1f))

                state.error != null && state.roles.isEmpty() ->
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
                        if (state.roles.isEmpty()) {
                            EmptyCreator(state.selectedTab)
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                            ) {
                                items(state.roles, key = { it.id }) { role ->
                                    RoleCard(
                                        role = role,
                                        onClick = { onRoleClick(role.id) },
                                        modifier = Modifier.padding(4.dp)
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
private fun EmptyCreator(tab: CreatorTab) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = when (tab) {
                CreatorTab.MY_ROLES -> "还没有创建角色"
                CreatorTab.FAVORITES -> "还没有收藏角色"
            },
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
