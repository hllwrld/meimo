package com.stx.meimo.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stx.meimo.data.remote.InstructionDto
import com.stx.meimo.ui.component.ErrorState
import com.stx.meimo.ui.component.LoadingIndicator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (state.showModelSelector && state.models.isNotEmpty()) {
        ModelSelectorSheet(
            models = state.models,
            categories = state.modelCategories,
            selectedModelId = state.selectedModel?.id,
            selectedCategoryId = state.selectedCategoryId,
            onSelect = { viewModel.selectModel(it) },
            onCategorySelect = { viewModel.selectModelCategory(it) },
            onDismiss = { viewModel.dismissModelSelector() }
        )
    }

    // User persona dialog
    if (state.showPersonaDialog) {
        PersonaDialog(
            currentPersona = state.userPersona,
            onConfirm = { viewModel.updatePersona(it) },
            onDismiss = { viewModel.dismissPersonaDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextButton(onClick = { viewModel.toggleModelSelector() }) {
                        Text(
                            text = state.selectedModel?.name ?: "选择模型",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = "选择模型",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                expandedHeight = 36.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    Box {
                        IconButton(onClick = { viewModel.toggleChatMenu() }) {
                            Icon(Icons.Default.Add, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = state.showChatMenu,
                            onDismissRequest = { viewModel.dismissChatMenu() }
                        ) {
                            DropdownMenuItem(
                                text = { Text("新的聊天") },
                                onClick = { viewModel.startNewChat() },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("用户人设") },
                                onClick = { viewModel.showPersonaDialog() },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("清空输入框") },
                                onClick = {
                                    viewModel.updateInput("")
                                    viewModel.dismissChatMenu()
                                },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(20.dp)) }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            when {
                state.isLoading && state.messages.isEmpty() ->
                    LoadingIndicator(modifier = Modifier.weight(1f))

                state.error != null && state.messages.isEmpty() ->
                    ErrorState(
                        message = state.error!!,
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.weight(1f)
                    )

                else -> {
                    val listState = rememberLazyListState()

                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                            lastVisibleItem != null &&
                                    lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 5
                        }
                    }

                    LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore && state.hasMore && !state.isLoadingMore) {
                            viewModel.loadMore()
                        }
                    }

                    // Messages list (reversed — newest at bottom)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        reverseLayout = true
                    ) {
                        if (state.isSending) {
                            item(key = "typing") {
                                TypingIndicator()
                            }
                        }

                        items(state.messages, key = { it.id }) { message ->
                            MessageBubble(
                                content = message.content ?: "",
                                isUser = message.direction == 1,
                                model = if (message.direction == 2) message.model else null,
                                isPrologue = message.id == -1L
                            )
                        }

                        if (state.isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }

                    // Instruction chips above input
                    if (state.instructions.isNotEmpty()) {
                        InstructionChips(
                            instructions = state.instructions,
                            onSelect = { viewModel.useInstruction(it) }
                        )
                    }

                    // User persona indicator
                    if (state.userPersona.isNotBlank()) {
                        Text(
                            text = "人设: ${state.userPersona.take(30)}${if (state.userPersona.length > 30) "..." else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    // Input bar
                    ChatInputBar(
                        text = state.inputText,
                        onTextChange = { viewModel.updateInput(it) },
                        onSend = { viewModel.sendMessage() },
                        isSending = state.isSending
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstructionChips(
    instructions: List<InstructionDto>,
    onSelect: (InstructionDto) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        instructions.forEach { instruction ->
            val tooltipState = rememberTooltipState()
            val scope = rememberCoroutineScope()

            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(
                            text = instruction.content?.take(100)?.let { "$it..." }
                                ?: "点击使用: ${instruction.name}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                state = tooltipState
            ) {
                AssistChip(
                    onClick = { onSelect(instruction) },
                    label = {
                        Text(
                            instruction.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun PersonaDialog(
    currentPersona: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentPersona) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("用户人设") },
        text = {
            Column {
                Text(
                    "设置你的角色人设，AI 会根据此人设与你互动",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如：我是一名18岁的大学生...") },
                    minLines = 3,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("输入消息...") },
            maxLines = 4,
            shape = MaterialTheme.shapes.large
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank() && !isSending
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    tint = if (text.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "正在输入...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
