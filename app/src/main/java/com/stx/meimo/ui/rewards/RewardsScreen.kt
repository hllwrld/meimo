package com.stx.meimo.ui.rewards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stx.meimo.data.remote.TaskDto
import com.stx.meimo.ui.component.LoadingIndicator
import com.stx.meimo.util.ServerConfig

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun RewardsScreen(viewModel: RewardsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDomainDialog by remember { mutableStateOf(false) }
    var domainToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.signInMessage) {
        state.signInMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSignInMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("奖励") }, expandedHeight = 36.dp, windowInsets = WindowInsets(0, 0, 0, 0))
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading && state.user == null ->
                LoadingIndicator(modifier = Modifier.padding(padding))
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Balance card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "当前余额",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${state.user?.balance ?: 0}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "电量",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sign-in button
                        Button(
                            onClick = { viewModel.signIn() },
                            enabled = !state.signedIn && !state.isSigningIn,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (state.signedIn) "今日已签到" else "每日签到"
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Task list
                        Text(
                            text = "任务列表",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (state.tasks.isEmpty()) {
                            Text(
                                text = "暂无任务",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            state.tasks.forEach { task ->
                                TaskItem(task = task)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Server selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "服务器",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { showAddDomainDialog = true }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "添加自定义域名",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.availableDomains.forEach { domain ->
                                val selected = domain == state.currentDomain
                                val isCustom = domain !in ServerConfig.presetDomainList
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.switchDomain(domain) },
                                    label = { Text(domain) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = if (selected) BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary
                                    ) else FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = false
                                    ),
                                    modifier = if (isCustom) {
                                        Modifier.combinedClickable(
                                            onClick = { viewModel.switchDomain(domain) },
                                            onLongClick = { domainToDelete = domain }
                                        )
                                    } else Modifier
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "切换后 API 立即生效，自定义域名长按可删除",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddDomainDialog) {
        var input by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDomainDialog = false },
            title = { Text("添加自定义域名") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("域名") },
                    placeholder = { Text("example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addCustomDomain(input)
                        showAddDomainDialog = false
                    },
                    enabled = input.isNotBlank()
                ) {
                    Text("保存并切换")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDomainDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    domainToDelete?.let { domain ->
        AlertDialog(
            onDismissRequest = { domainToDelete = null },
            title = { Text("删除域名") },
            text = { Text("确定删除 $domain？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeCustomDomain(domain)
                        domainToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { domainToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun TaskItem(task: TaskDto) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name ?: "任务",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (task.description != null) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (task.target > 0) {
                    Text(
                        text = "进度: ${task.progress}/${task.target}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (task.reward > 0) {
                Text(
                    text = "+${task.reward}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
