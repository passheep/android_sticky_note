@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.passheep.sticky_note.feature.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passheep.sticky_note.StickyNoteTokens
import com.passheep.sticky_note.core.model.RepeatType
import com.passheep.sticky_note.core.model.Todo
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val TODO_PAGE_SIZE = 80

/**
 * 扁平化、不可变的列表项模型：将文案、日期标签、分组信息预先算出，确保成百上千条滚动时零冗余重组
 */
@Immutable
private sealed interface TodoListEntry {
    val stableKey: String

    @Immutable
    data class SectionHeader(
        val key: String,
        val title: String,
        val isFirst: Boolean,
        val isOverdue: Boolean = false,
    ) : TodoListEntry {
        override val stableKey: String = "header_$key"
    }

    @Immutable
    data class TodoItem(
        val todo: Todo,
        val priorityLabel: String,
        val repeatLabel: String?,
        val extraTimeLabel: String?,
        val rightDueText: String?,
        val isOverdue: Boolean,
        val isToday: Boolean,
        val showBottomDivider: Boolean,
    ) : TodoListEntry {
        // 使用纯 localId 作为稳定 key，待办在分组间完成/取消完成时可平滑播放 animateItem 位移动画
        override val stableKey: String = todo.localId
    }
}

/**
 * 暖纸墨韵待办主页（精简顶栏、丝滑完成动画、左滑展示复制/删除按钮、大列表增量懒加载与新建自动滚动定位）
 */
@Composable
fun TodosScreen(
    uiState: AppUiState,
    pendingScrollTodoId: String?,
    onConsumePendingScroll: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenSettings: () -> Unit,
    onFilterChange: (TodoFilter) -> Unit,
    onQuickTitleChange: (String) -> Unit,
    onQuickDueDateChange: (LocalDate?) -> Unit,
    onQuickSubmit: () -> Unit,
    onOpenQuickEditor: () -> Unit,
    onOpenTodo: (Todo) -> Unit,
    onDismissEditor: () -> Unit,
    onUpdateEditor: ((TodoEditDraft) -> TodoEditDraft) -> Unit,
    onSaveEditor: () -> Unit,
    onToggleTodo: (Todo) -> Unit,
    onCopyTodo: (Todo) -> Unit,
    onDeleteTodo: (Todo) -> Unit,
    onRefresh: () -> Unit,
) {
    val colors = StickyNoteTokens.colors
    var showEditDatePicker by rememberSaveable { mutableStateOf(false) }
    var showQuickDatePicker by rememberSaveable { mutableStateOf(false) }
    var composerFocused by rememberSaveable { mutableStateOf(false) }
    var revealedTodoId by remember { mutableStateOf<String?>(null) }
    var highlightedTodoId by remember { mutableStateOf<String?>(null) }
    var visibleLimit by rememberSaveable(uiState.todoFilter) { mutableIntStateOf(TODO_PAGE_SIZE) }

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    val showComposerActions = composerFocused && imeVisible

    val today = remember { LocalDate.now(BeijingZoneId) }
    val totalCount = uiState.todos.size
    val activeCount = remember(uiState.todos) { uiState.todos.count { !it.completed } }
    val completedCount = remember(uiState.todos) { uiState.todos.count { it.completed } }

    // 一次性预计算全部扁平不可变条目
    val allEntries = remember(uiState.todos, uiState.todoFilter, today) {
        buildFlatTodoEntries(
            todos = uiState.todos,
            filter = uiState.todoFilter,
            today = today,
        )
    }

    // 增量懒加载切片：面对成百上千条待办时按每批 80 条自动静默加载，保持 120Hz 顺滑滚动
    val visibleEntries = remember(allEntries, visibleLimit) {
        if (allEntries.size <= visibleLimit) allEntries else allEntries.subList(0, visibleLimit)
    }
    val hasMoreEntries = allEntries.size > visibleEntries.size

    // 在协程内通过 snapshotFlow 监听滚动状态与触底加载，绝不在组合阶段读 listState，避免每帧滚动触发重组
    val latestTotalEntries by rememberUpdatedState(allEntries.size)
    val latestVisibleCount by rememberUpdatedState(visibleEntries.size)
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling && revealedTodoId != null) {
                    revealedTodoId = null
                }
            }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            val total = latestTotalEntries
            val currentVisible = latestVisibleCount
            if (currentVisible >= total) {
                false
            } else {
                val lastIdx = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastIdx >= (currentVisible - 16).coerceAtLeast(0)
            }
        }
            .distinctUntilChanged()
            .collect { needMore ->
                if (needMore) {
                    visibleLimit = (visibleLimit + TODO_PAGE_SIZE).coerceAtMost(latestTotalEntries)
                }
            }
    }

    LaunchedEffect(uiState.todoFilter) {
        revealedTodoId = null
        if (pendingScrollTodoId == null) {
            listState.scrollToItem(0)
        }
    }

    // 独立高亮自动消隐计时器（避免与 pendingScrollTodoId 的取消周期耦合）
    LaunchedEffect(highlightedTodoId) {
        if (highlightedTodoId != null) {
            delay(1400)
            highlightedTodoId = null
        }
    }

    // 新建、修改或复制待办后，先确保展开到目标条目并完成平滑滚动，最后再消费事件防止协程自取消
    LaunchedEffect(pendingScrollTodoId, allEntries, visibleLimit) {
        val targetId = pendingScrollTodoId ?: return@LaunchedEffect
        val targetIndex = allEntries.indexOfFirst { it is TodoListEntry.TodoItem && it.todo.localId == targetId }
        if (targetIndex < 0) return@LaunchedEffect
        if (targetIndex >= visibleLimit) {
            visibleLimit = targetIndex + TODO_PAGE_SIZE
            return@LaunchedEffect
        }
        delay(60)
        runCatching { listState.animateScrollToItem(index = targetIndex, scrollOffset = -64) }
        highlightedTodoId = targetId
        onConsumePendingScroll()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshingTodos,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                // 顶部极简功能栏：当日日期 + 右上设备胶囊与设置按钮
                TodosMinimalTopBar(
                    dateLabel = today.toHeaderDateLabel(),
                    deviceLabel = uiState.selectedDeviceLabel,
                    isCloudReady = uiState.isCloudReady,
                    showSettingsBadge = uiState.showSettingsBadge,
                    onOpenDevices = onOpenDevices,
                    onOpenSettings = onOpenSettings,
                )

                // 直接紧贴顶栏展示筛选 Tab，最大化列表可视内容
                PaperFilterTabs(
                    selected = uiState.todoFilter,
                    totalCount = totalCount,
                    activeCount = activeCount,
                    completedCount = completedCount,
                    onSelect = onFilterChange,
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 10.dp,
                        bottom = if (showComposerActions) 152.dp else 112.dp,
                    ),
                ) {
                    if (visibleEntries.isEmpty()) {
                        item(key = "empty_state", contentType = "empty") {
                            Spacer(modifier = Modifier.height(8.dp))
                            PaperEmptyCard(
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .animateItem(
                                        fadeInSpec = tween(220),
                                        fadeOutSpec = tween(160),
                                    ),
                                title = when {
                                    !uiState.isCloudReady -> "尚未连接极趣云平台"
                                    uiState.todoFilter == TodoFilter.COMPLETED -> "还没有已完成的待办"
                                    uiState.todoFilter == TodoFilter.ACTIVE -> "所有待办都已完成"
                                    else -> "纸面还空着，记下第一条吧"
                                },
                                body = when {
                                    !uiState.isCloudReady -> "前往设置中心开启平台接入并填写 API 密钥，即可与墨水屏同步待办。"
                                    else -> "在底部输入框随时新建便利贴，或下拉刷新同步最新云端清单。"
                                },
                                actionLabel = if (!uiState.isCloudReady) "前往设置中心" else null,
                                onAction = if (!uiState.isCloudReady) onOpenSettings else null,
                            )
                        }
                    } else {
                        items(
                            items = visibleEntries,
                            key = TodoListEntry::stableKey,
                            contentType = { entry ->
                                when (entry) {
                                    is TodoListEntry.SectionHeader -> "section_header"
                                    is TodoListEntry.TodoItem -> "paper_todo"
                                }
                            },
                        ) { entry ->
                            when (entry) {
                                is TodoListEntry.SectionHeader -> {
                                    Text(
                                        text = entry.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem(
                                                fadeInSpec = tween(180),
                                                fadeOutSpec = tween(150),
                                                placementSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                ),
                                            )
                                            .padding(
                                                start = 22.dp,
                                                end = 22.dp,
                                                top = if (entry.isFirst) 4.dp else 18.dp,
                                                bottom = 4.dp,
                                            ),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.6.sp,
                                        ),
                                        color = if (entry.isOverdue) colors.urgent else colors.inkMuted,
                                    )
                                }

                                is TodoListEntry.TodoItem -> {
                                    SwipeRevealTodoRow(
                                        model = entry,
                                        isRevealed = revealedTodoId == entry.todo.localId,
                                        isHighlighted = highlightedTodoId == entry.todo.localId,
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = tween(210),
                                            fadeOutSpec = tween(160),
                                            placementSpec = spring(
                                                stiffness = Spring.StiffnessMediumLow,
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                            ),
                                        ),
                                        onRevealChange = { expanded ->
                                            revealedTodoId = if (expanded) {
                                                entry.todo.localId
                                            } else if (revealedTodoId == entry.todo.localId) {
                                                null
                                            } else {
                                                revealedTodoId
                                            }
                                        },
                                        onOpen = {
                                            if (revealedTodoId != null) {
                                                revealedTodoId = null
                                            } else {
                                                onOpenTodo(entry.todo)
                                            }
                                        },
                                        onToggle = {
                                            revealedTodoId = null
                                            onToggleTodo(entry.todo)
                                        },
                                        onCopy = {
                                            revealedTodoId = null
                                            onCopyTodo(entry.todo)
                                        },
                                        onDelete = {
                                            revealedTodoId = null
                                            onDeleteTodo(entry.todo)
                                        },
                                    )
                                }
                            }
                        }

                        if (hasMoreEntries) {
                            item(key = "load_more_hint", contentType = "footer") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "已展示 ${visibleEntries.count { it is TodoListEntry.TodoItem }} / ${allEntries.count { it is TodoListEntry.TodoItem }} 条，继续下滑加载更多",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                        color = colors.inkMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 底部纯净悬浮快捷输入槽（无半透明灰雾底带，直接透出纸张/网格画布）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            PaperQuickComposer(
                draft = uiState.quickDraft,
                isExpanded = showComposerActions,
                onTitleChange = onQuickTitleChange,
                onDateClick = { showQuickDatePicker = true },
                onFocusChanged = { composerFocused = it },
                onCancel = {
                    composerFocused = false
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                },
                onMore = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    composerFocused = false
                    onOpenQuickEditor()
                },
                onSubmit = {
                    onQuickSubmit()
                    composerFocused = false
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                },
            )
        }
    }

    if (showQuickDatePicker) {
        val quickDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.quickDraft.dueDate
                ?.toDatePickerUtcMillis()
                ?: LocalDate.now(BeijingZoneId).toDatePickerUtcMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showQuickDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onQuickDueDateChange(quickDatePickerState.selectedDateMillis?.toDatePickerLocalDate())
                        showQuickDatePicker = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onQuickDueDateChange(null)
                            showQuickDatePicker = false
                        },
                    ) {
                        Text("清空")
                    }
                    TextButton(onClick = { showQuickDatePicker = false }) {
                        Text("取消")
                    }
                }
            },
        ) {
            DatePicker(state = quickDatePickerState)
        }
    }

    uiState.editDraft?.let { editor ->
        EditTodoSheet(
            draft = editor,
            onDismiss = onDismissEditor,
            onUpdate = onUpdateEditor,
            onSave = onSaveEditor,
            onDelete = {
                if (editor.remoteId == null) {
                    onDismissEditor()
                } else {
                    uiState.todos.firstOrNull { it.localId == editor.localId }?.let(onDeleteTodo)
                }
            },
            onShowDatePicker = { showEditDatePicker = true },
        )
    }

    if (showEditDatePicker && uiState.editDraft != null) {
        val editDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.editDraft.dueDate
                ?.toDatePickerUtcMillis()
                ?: LocalDate.now(BeijingZoneId).toDatePickerUtcMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showEditDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateEditor { current ->
                            current.copy(dueDate = editDatePickerState.selectedDateMillis?.toDatePickerLocalDate())
                        }
                        showEditDatePicker = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                Row {
                    if (uiState.editDraft.dueDate != null) {
                        TextButton(
                            onClick = {
                                onUpdateEditor { current -> current.copy(dueDate = null) }
                                showEditDatePicker = false
                            },
                        ) {
                            Text("清空")
                        }
                    }
                    TextButton(onClick = { showEditDatePicker = false }) {
                        Text("取消")
                    }
                }
            },
        ) {
            DatePicker(state = editDatePickerState)
        }
    }
}

private fun buildFlatTodoEntries(
    todos: List<Todo>,
    filter: TodoFilter,
    today: LocalDate,
): List<TodoListEntry> {
    fun List<Todo>.toRowModels(): List<TodoListEntry.TodoItem> = mapIndexed { idx, todo ->
        val isOverdue = !todo.completed && todo.dueDate != null && todo.dueDate.isBefore(today)
        val isToday = !todo.completed && todo.dueDate == today
        val rightDueText = when {
            todo.completed -> null
            todo.dueDate == today && todo.dueTime != null -> todo.dueTime.toString()
            todo.dueDate != null -> todo.dueDate.toTodoDueDateLabel()
            todo.dueTime != null -> todo.dueTime.toString()
            else -> null
        }
        val extraTimeLabel = if (!todo.completed && todo.dueDate != null && todo.dueDate != today && todo.dueTime != null) {
            todo.dueTime.toString()
        } else {
            null
        }
        TodoListEntry.TodoItem(
            todo = todo,
            priorityLabel = if (todo.completed) "已完成 · ${todo.priority.toPriorityLabel()}" else todo.priority.toPriorityLabel(),
            repeatLabel = todo.repeatSummaryLabel(),
            extraTimeLabel = extraTimeLabel,
            rightDueText = rightDueText,
            isOverdue = isOverdue,
            isToday = isToday,
            showBottomDivider = idx < lastIndex,
        )
    }

    if (filter == TodoFilter.COMPLETED) {
        val done = todos.filter { it.completed }
        return done.toRowModels()
    }

    val active = todos.filter { !it.completed }
    val overdueItems = active.filter { it.dueDate != null && it.dueDate.isBefore(today) }
    val todayItems = active.filter { it.dueDate == today }
    val upcomingItems = active.filter { it.dueDate == null || it.dueDate.isAfter(today) }
    val completedItems = if (filter == TodoFilter.ALL) todos.filter { it.completed } else emptyList()

    return buildList {
        var firstHeader = true
        if (overdueItems.isNotEmpty()) {
            add(TodoListEntry.SectionHeader("overdue", "已逾期", isFirst = firstHeader, isOverdue = true))
            firstHeader = false
            addAll(overdueItems.toRowModels())
        }
        if (todayItems.isNotEmpty()) {
            add(TodoListEntry.SectionHeader("today", "今天", isFirst = firstHeader))
            firstHeader = false
            addAll(todayItems.toRowModels())
        }
        if (upcomingItems.isNotEmpty()) {
            val showHeader = overdueItems.isNotEmpty() || todayItems.isNotEmpty() || completedItems.isNotEmpty()
            if (showHeader) {
                add(TodoListEntry.SectionHeader("upcoming", "后续计划", isFirst = firstHeader))
                firstHeader = false
            }
            addAll(upcomingItems.toRowModels())
        }
        if (completedItems.isNotEmpty()) {
            add(TodoListEntry.SectionHeader("completed", "已完成", isFirst = firstHeader))
            addAll(completedItems.toRowModels())
        }
    }
}

@Composable
private fun TodosMinimalTopBar(
    dateLabel: String,
    deviceLabel: String?,
    isCloudReady: Boolean,
    showSettingsBadge: Boolean,
    onOpenDevices: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = StickyNoteTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 18.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum",
            ),
            color = colors.inkSecondary,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                onClick = onOpenDevices,
                shape = CircleShape,
                color = colors.surface,
                border = BorderStroke(0.8.dp, colors.borderSubtle),
                shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
                modifier = Modifier.height(36.dp),
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 172.dp)
                        .padding(start = 11.dp, end = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCloudReady && !deviceLabel.isNullOrBlank()) colors.accent else colors.inkMuted.copy(alpha = 0.5f),
                            ),
                    )
                    Text(
                        text = deviceLabel?.takeIf { it.isNotBlank() } ?: "选择设备",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "切换设备",
                        tint = colors.inkSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            Box {
                Surface(
                    onClick = onOpenSettings,
                    shape = CircleShape,
                    color = colors.surface,
                    border = BorderStroke(0.8.dp, colors.borderSubtle),
                    shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "设置中心",
                            tint = colors.inkPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                if (showSettingsBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 1.dp, end = 1.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.badgeDot),
                    )
                }
            }
        }
    }
}

@Composable
private fun PaperFilterTabs(
    selected: TodoFilter,
    totalCount: Int,
    activeCount: Int,
    completedCount: Int,
    onSelect: (TodoFilter) -> Unit,
) {
    val colors = StickyNoteTokens.colors
    val tabs = listOf(
        Triple(TodoFilter.ALL, "全部", totalCount),
        Triple(TodoFilter.ACTIVE, "未完成", activeCount),
        Triple(TodoFilter.COMPLETED, "已完成", completedCount),
    )

    Column(modifier = Modifier.padding(top = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            tabs.forEach { (filter, label, count) ->
                val isSelected = filter == selected
                val indicatorWidth by animateDpAsState(
                    targetValue = if (isSelected) 32.dp else 0.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "tabIndicator",
                )
                Column(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(filter) },
                        )
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(bottom = 10.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) colors.inkPrimary else colors.inkMuted,
                        )
                        AnimatedContent(
                            targetState = count,
                            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(120)) },
                            label = "tabCount",
                        ) { targetCount ->
                            Text(
                                text = "$targetCount",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) colors.accent else colors.inkMuted,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(indicatorWidth)
                            .height(2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.accent),
                    )
                }
            }
        }
        PaperHairline()
    }
}

/**
 * 左滑展开【复制】【删除】操作按钮的待办行容器：
 * 仅左滑露出操作按钮，由用户明确点击按钮后再执行复制或删除，彻底避免滑动误触。
 */
@Composable
private fun SwipeRevealTodoRow(
    model: TodoListEntry.TodoItem,
    isRevealed: Boolean,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
    onRevealChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = StickyNoteTokens.colors
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // 按钮宽 62dp×2 + 间距 8dp + 右边距 16dp + 复制按钮左侧留白 16dp = 164dp，避免遮挡「复制」左侧
    val revealWidthDp = 164.dp
    val maxRevealPx = remember(density) { with(density) { revealWidthDp.toPx() } }
    val offsetX = remember(model.todo.localId) { Animatable(0f) }

    LaunchedEffect(isRevealed, maxRevealPx) {
        val target = if (isRevealed) -maxRevealPx else 0f
        if (offsetX.value != target) {
            offsetX.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    val highlightBg by animateColorAsState(
        targetValue = if (isHighlighted) colors.accentSoft.copy(alpha = 0.65f) else Color.Transparent,
        animationSpec = tween(durationMillis = 360),
        label = "todoRowHighlight",
    )
    val isOffsetActive = offsetX.value < -1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(highlightBg),
    ) {
        // 仅在实际左滑展开时渲染右侧操作按钮，静止状态完全不干扰网格/点阵纸底
        if (isOffsetActive) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onCopy,
                    shape = RoundedCornerShape(14.dp),
                    color = colors.accent,
                    modifier = Modifier
                        .width(62.dp)
                        .fillMaxHeight(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "复制",
                            tint = colors.onAccent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "复制",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            color = colors.onAccent,
                        )
                    }
                }

                Surface(
                    onClick = onDelete,
                    shape = RoundedCornerShape(14.dp),
                    color = colors.urgent,
                    modifier = Modifier
                        .width(62.dp)
                        .fillMaxHeight(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "删除",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "删除",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            color = Color.White,
                        )
                    }
                }
            }
        }

        // 前景可滑动行内容
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .background(if (isOffsetActive) colors.canvas else Color.Transparent)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val next = (offsetX.value + delta).coerceIn(-maxRevealPx, 0f)
                            offsetX.snapTo(next)
                        }
                    },
                    onDragStopped = { velocity ->
                        val shouldOpen = velocity < -360f || (velocity <= 360f && offsetX.value < -maxRevealPx * 0.42f)
                        onRevealChange(shouldOpen)
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = if (shouldOpen) -maxRevealPx else 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                    },
                )
                .padding(horizontal = 22.dp),
        ) {
            PaperTodoRow(
                model = model,
                onOpen = onOpen,
                onToggle = onToggle,
            )
        }
    }
}

/**
 * 纸面待办行：修复左侧小圆圈同心居中防裁切，增加完成/取消完成弹性动效
 */
@Composable
private fun PaperTodoRow(
    model: TodoListEntry.TodoItem,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = StickyNoteTokens.colors
    val todo = model.todo
    val isUrgent = todo.priority == 2
    val isImportant = todo.priority == 1

    val ringColor = when {
        todo.completed -> colors.completed
        isUrgent -> colors.urgent
        isImportant -> colors.important
        else -> colors.inkMuted
    }
    val ringFill = when {
        todo.completed -> colors.completed
        isUrgent -> colors.urgent.copy(alpha = 0.09f)
        isImportant -> colors.important.copy(alpha = 0.09f)
        else -> Color.Transparent
    }
    val titleColor = if (todo.completed) colors.inkMuted else colors.inkPrimary
    // 仅保留轻量勾选弹性缩放，减少超大列表高速滑动时的每行动画状态分配开销
    val checkScale by animateFloatAsState(
        targetValue = if (todo.completed) 1.0f else 0.96f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "checkScale",
    )

    val rightDueColor = when {
        model.isOverdue || (isUrgent && model.isToday) -> colors.urgent
        model.isToday -> colors.accent
        else -> colors.inkMuted
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // 38dp 同心正圆触控热区，内层 21dp 圆圈完全居中，彻底修复左上角裁切缺角问题
            Box(
                modifier = Modifier
                    .offset(x = (-8).dp, y = (-6).dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .scale(checkScale)
                        .size(21.dp)
                        .clip(CircleShape)
                        .background(ringFill)
                        .border(width = 1.75.dp, color = ringColor, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = todo.completed,
                        enter = scaleIn(
                            initialScale = 0.4f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        ) + fadeIn(tween(120)),
                        exit = scaleOut(targetScale = 0.4f, animationSpec = tween(100)) + fadeOut(tween(100)),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "取消完成",
                            tint = colors.onAccent,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
            ) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 23.sp,
                    ),
                    color = titleColor,
                    textDecoration = if (todo.completed) TextDecoration.LineThrough else TextDecoration.None,
                )
                if (todo.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val priorityColor = when {
                        todo.completed -> colors.inkMuted
                        isUrgent -> colors.urgent
                        isImportant -> colors.important
                        else -> colors.inkSecondary
                    }
                    Text(
                        text = model.priorityLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (!todo.completed && (isUrgent || isImportant)) FontWeight.SemiBold else FontWeight.Normal,
                        color = priorityColor,
                    )
                    if (!model.repeatLabel.isNullOrBlank()) {
                        Text(text = "·", style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                        Text(
                            text = model.repeatLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.inkSecondary,
                        )
                    }
                    if (!model.extraTimeLabel.isNullOrBlank()) {
                        Text(text = "·", style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                        Text(
                            text = model.extraTimeLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = colors.inkSecondary,
                        )
                    }
                }
            }

            if (!model.rightDueText.isNullOrBlank()) {
                Text(
                    text = model.rightDueText,
                    modifier = Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (model.isToday || model.isOverdue) FontWeight.SemiBold else FontWeight.Medium,
                        fontFeatureSettings = "tnum",
                    ),
                    color = rightDueColor,
                    maxLines = 1,
                )
            }
        }
        if (model.showBottomDivider) {
            PaperHairline(startIndent = 30.dp)
        }
    }
}

/**
 * 底部悬浮暖纸快速输入胶囊
 */
@Composable
private fun PaperQuickComposer(
    draft: QuickTodoDraft,
    isExpanded: Boolean,
    onTitleChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onMore: () -> Unit,
    onSubmit: () -> Unit,
) {
    val colors = StickyNoteTokens.colors
    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 24.dp else 28.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "composerCorner",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(cornerRadius),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.accentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(17.dp),
                    )
                }

                BasicTextField(
                    value = draft.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { state -> onFocusChanged(state.isFocused) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.inkPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    cursorBrush = SolidColor(colors.accent),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                        ) {
                            if (draft.title.isBlank()) {
                                Text(
                                    text = "记一条新便利贴...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.inkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                Surface(
                    onClick = onDateClick,
                    shape = CircleShape,
                    color = if (draft.dueDate != null) colors.accent else colors.surfaceMuted,
                    modifier = Modifier.height(36.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DateRange,
                            contentDescription = null,
                            tint = if (draft.dueDate != null) colors.onAccent else colors.inkSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = draft.dueDate.quickDateLabel(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum",
                            ),
                            color = if (draft.dueDate != null) colors.onAccent else colors.inkSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(180)) + fadeIn(tween(160)),
                exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(tween(120)),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PaperHairline()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onCancel) {
                            Text("取消", color = colors.inkSecondary)
                        }
                        TextButton(onClick = onMore) {
                            Text("详细编辑", color = colors.inkPrimary)
                        }
                        Surface(
                            onClick = onSubmit,
                            shape = CircleShape,
                            color = colors.accent,
                            modifier = Modifier.height(36.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 18.dp),
                            ) {
                                Text(
                                    text = "创建",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.onAccent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 暖纸风格待办编辑/新增底部弹层：新建模式下自动聚焦标题并弹出输入法
 */
@Composable
private fun EditTodoSheet(
    draft: TodoEditDraft,
    onDismiss: () -> Unit,
    onUpdate: ((TodoEditDraft) -> TodoEditDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onShowDatePicker: () -> Unit,
) {
    val colors = StickyNoteTokens.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 新建待办打开详细编辑弹层后，自动聚焦标题框并拉起系统输入法
    LaunchedEffect(draft.isCreateMode) {
        if (draft.isCreateMode) {
            delay(180)
            runCatching {
                titleFocusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = colors.canvas,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding(),
            containerColor = colors.canvas,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.hairline),
                    shadowElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val isCreate = draft.isCreateMode || draft.remoteId == null
                        PaperSecondaryButton(
                            text = if (isCreate) "取消" else "删除",
                            destructive = !isCreate,
                            onClick = {
                                if (isCreate) {
                                    onDismiss()
                                } else {
                                    showDeleteConfirmation = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        PaperPrimaryButton(
                            text = "保存",
                            onClick = onSave,
                            modifier = Modifier.weight(1.4f),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.borderSubtle),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (draft.isCreateMode) "新增便利贴" else "编辑便利贴",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.inkPrimary,
                )

                Spacer(modifier = Modifier.height(16.dp))
                PaperSectionCard(contentPadding = PaddingValues(16.dp)) {
                    PaperTextField(
                        value = draft.title,
                        onValueChange = { value -> onUpdate { it.copy(title = value) } },
                        label = "标题",
                        placeholder = "要做什么...",
                        focusRequester = titleFocusRequester,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PaperTextField(
                        value = draft.description,
                        onValueChange = { value -> onUpdate { it.copy(description = value) } },
                        label = "备注（选填）",
                        placeholder = "补充细节或清单要点",
                        singleLine = false,
                        minLines = 2,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                PaperGroupLabel("日程与优先级")
                PaperSectionCard(
                    modifier = Modifier.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PaperPickerField(
                            modifier = Modifier.weight(1f),
                            label = "日期",
                            value = draft.dueDate?.toString(),
                            placeholder = "不设置",
                            trailingIcon = Icons.Rounded.DateRange,
                            onClick = onShowDatePicker,
                        )
                        PaperDropdownField(
                            modifier = Modifier.weight(1f),
                            label = "时间",
                            value = draft.dueTimeText.takeIf { it.isNotBlank() },
                            placeholder = "不设置",
                            options = buildList {
                                add(PaperMenuOption("", "不设置"))
                                addAll(TimeOptions.map { PaperMenuOption(it, it) })
                            },
                            onSelect = { value -> onUpdate { it.copy(dueTimeText = value) } },
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "优先级",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.inkSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PaperSegmentedControl(
                        options = listOf(0 to "普通", 1 to "重要", 2 to "紧急"),
                        selected = draft.priority,
                        onSelect = { value -> onUpdate { it.copy(priority = value) } },
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    PaperDropdownField(
                        modifier = Modifier.fillMaxWidth(),
                        label = "重复周期",
                        value = (draft.repeatType ?: RepeatType.NONE).toRepeatTypeLabel(),
                        placeholder = "不重复",
                        options = listOf(
                            PaperMenuOption(RepeatType.NONE, "不重复"),
                            PaperMenuOption(RepeatType.DAILY, "每天"),
                            PaperMenuOption(RepeatType.WEEKLY, "每周"),
                            PaperMenuOption(RepeatType.MONTHLY, "每月"),
                            PaperMenuOption(RepeatType.YEARLY, "每年"),
                        ),
                        onSelect = { value -> onUpdate { it.copy(repeatType = value) } },
                    )

                    val activeRepeat = draft.repeatType ?: RepeatType.NONE
                    AnimatedVisibility(
                        visible = activeRepeat == RepeatType.WEEKLY ||
                            activeRepeat == RepeatType.MONTHLY ||
                            activeRepeat == RepeatType.YEARLY,
                        enter = expandVertically(tween(200)) + fadeIn(tween(180)),
                        exit = shrinkVertically(tween(160)) + fadeOut(tween(120)),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            when (activeRepeat) {
                                RepeatType.WEEKLY -> {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    PaperDropdownField(
                                        modifier = Modifier.fillMaxWidth(),
                                        label = "每周几",
                                        value = draft.repeatWeekday?.toWeekdayLabel(),
                                        placeholder = "选择周几",
                                        options = (0..6).map { PaperMenuOption(it, it.toWeekdayLabel()) },
                                        onSelect = { value -> onUpdate { it.copy(repeatWeekday = value) } },
                                    )
                                }

                                RepeatType.MONTHLY -> {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    PaperDropdownField(
                                        modifier = Modifier.fillMaxWidth(),
                                        label = "每月几号",
                                        value = draft.repeatDay?.let { "$it 号" },
                                        placeholder = "选择日期",
                                        options = (1..31).map { PaperMenuOption(it, "$it 号") },
                                        onSelect = { value -> onUpdate { it.copy(repeatDay = value) } },
                                    )
                                }

                                RepeatType.YEARLY -> {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        PaperDropdownField(
                                            modifier = Modifier.weight(1f),
                                            label = "月份",
                                            value = draft.repeatMonth?.let { "$it 月" },
                                            placeholder = "选择月份",
                                            options = (1..12).map { PaperMenuOption(it, "$it 月") },
                                            onSelect = { value -> onUpdate { it.copy(repeatMonth = value) } },
                                        )
                                        PaperDropdownField(
                                            modifier = Modifier.weight(1f),
                                            label = "日期",
                                            value = draft.repeatDay?.let { "$it 号" },
                                            placeholder = "选择日期",
                                            options = (1..31).map { PaperMenuOption(it, "$it 号") },
                                            onSelect = { value -> onUpdate { it.copy(repeatDay = value) } },
                                        )
                                    }
                                }

                                else -> Unit
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                PaperGroupLabel("同步信息")
                PaperSectionCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    PaperInfoRow("关联设备", draft.deviceName.ifBlank { "未关联设备" })
                    PaperHairline()
                    PaperInfoRow("创建时间", draft.createdAtLabel.ifBlank { "创建后生成" })
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = colors.surface,
            title = { Text("确认删除", color = colors.inkPrimary) },
            text = { Text("删除后将同步删除云端待办，确定继续吗？", color = colors.inkSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                ) {
                    Text("删除", color = colors.urgent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("取消", color = colors.inkSecondary)
                }
            },
        )
    }
}
