@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.passheep.sticky_note.feature.app

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.passheep.sticky_note.core.model.Device
import com.passheep.sticky_note.core.model.RepeatType
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.settings.ThemeMode
import com.passheep.sticky_note.widget.requestPinStickyNoteWidget
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val TodoPendingColor = Color(0xFFB45309)
private val TodoCompletedColor = Color(0xFF047857)
private val TimeOptions = buildList {
    for (hour in 0..23) {
        add(String.format("%02d:00", hour))
        add(String.format("%02d:30", hour))
    }
}
private val LastSyncFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val BeijingZoneId: ZoneId = ZoneId.of("Asia/Shanghai")

private val PanelBackground: Color
    @Composable get() = if (isPanelDarkTheme()) Color(0xFF121A28) else Color.White

private val PanelTextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

private val PanelTextSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val PanelBorder: Color
    @Composable get() = if (isPanelDarkTheme()) MaterialTheme.colorScheme.outline.copy(alpha = 0.42f) else Color(0xFFE5E7EB)

private val PanelBorderStrong: Color
    @Composable get() = if (isPanelDarkTheme()) MaterialTheme.colorScheme.outline.copy(alpha = 0.62f) else Color(0xFFD1D5DB)

@Composable
fun StickyNoteAppShell(
    navController: NavHostController,
    currentRoute: String,
    uiState: AppUiState,
    onPageEntered: (String) -> Unit,
    onFilterChange: (TodoFilter) -> Unit,
    onQuickTitleChange: (String) -> Unit,
    onQuickDescriptionChange: (String) -> Unit,
    onQuickDueDateChange: (LocalDate?) -> Unit,
    onQuickDueTimeChange: (String) -> Unit,
    onQuickPriorityChange: (Int) -> Unit,
    onQuickRepeatTypeChange: (RepeatType?) -> Unit,
    onQuickRepeatWeekdayChange: (Int?) -> Unit,
    onQuickRepeatMonthChange: (Int?) -> Unit,
    onQuickRepeatDayChange: (Int?) -> Unit,
    onQuickSubmit: () -> Unit,
    onOpenQuickEditor: () -> Unit,
    onOpenTodo: (Todo) -> Unit,
    onDismissEditor: () -> Unit,
    onUpdateEditor: ((TodoEditDraft) -> TodoEditDraft) -> Unit,
    onSaveEditor: () -> Unit,
    onToggleTodo: (Todo) -> Unit,
    onDeleteTodo: (Todo) -> Unit,
    onRefreshTodos: () -> Unit,
    onSetPlatformEnabled: (Boolean) -> Unit,
    onCommitPlatformUrl: (String) -> Unit,
    onCommitApiKey: (String) -> Unit,
    onSetSyncIntervalSeconds: (Int) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetWidgetThemeMode: (ThemeMode) -> Unit,
    onSetWidgetTransparency: (Float) -> Unit,
    onSetWidgetColorfulTextEnabled: (Boolean) -> Unit,
    onSetQuickTodoDefaultTodayEnabled: (Boolean) -> Unit,
    onTestPlatformConnection: (String, String) -> Unit,
    onRefreshDevices: () -> Unit,
    onSelectDevice: (String) -> Unit,
) {
    val drawerState = androidx.compose.material3.rememberDrawerState(
        initialValue = androidx.compose.material3.DrawerValue.Closed,
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentRoute) {
        onPageEntered(currentRoute)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    Text(
                        text = "智能便利贴",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                DrawerItem(
                    selected = currentRoute == AppRoute.TODOS.route,
                    label = "待办清单",
                    icon = Icons.AutoMirrored.Rounded.ViewList,
                    showDot = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigateSingleTop(AppRoute.TODOS.route)
                        }
                    },
                )
                DrawerItem(
                    selected = currentRoute == AppRoute.SETTINGS.route,
                    label = "设置中心",
                    icon = Icons.Rounded.Settings,
                    showDot = uiState.showSettingsBadge,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigateSingleTop(AppRoute.SETTINGS.route)
                        }
                    },
                )
                DrawerItem(
                    selected = currentRoute == AppRoute.ABOUT.route,
                    label = "关于软件",
                    icon = Icons.Rounded.Info,
                    showDot = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigateSingleTop(AppRoute.ABOUT.route)
                        }
                    },
                )
                DrawerItem(
                    selected = currentRoute == AppRoute.DEVICE_SWITCH.route,
                    label = "切换设备",
                    icon = Icons.Rounded.Devices,
                    showDot = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigateSingleTop(AppRoute.DEVICE_SWITCH.route)
                        }
                    },
                )
            }
        },
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = AppRoute.entries.firstOrNull { it.route == currentRoute }?.title ?: "智能便利贴",
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "打开菜单")
                        }
                    },
                    actions = {
                        if (currentRoute == AppRoute.TODOS.route) {
                            if (uiState.selectedDeviceLabel.isNullOrBlank()) {
                                TextButton(onClick = { navController.navigateSingleTop(AppRoute.DEVICE_SWITCH.route) }) {
                                    Text("选择设备")
                                }
                            } else {
                                TextButton(
                                    onClick = { navController.navigateSingleTop(AppRoute.DEVICE_SWITCH.route) },
                                    modifier = Modifier.widthIn(max = 160.dp),
                                    contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 16.dp, bottom = 4.dp),
                                ) {
                                    Text(
                                        text = uiState.selectedDeviceLabel.orEmpty(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    },
                )
            },
        ) { innerPadding ->
            AppBackdrop(
                modifier = Modifier.padding(innerPadding),
                showDecor = currentRoute != AppRoute.TODOS.route,
            ) {
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.TODOS.route,
                ) {
                    composable(AppRoute.TODOS.route) {
                        TodosPage(
                            uiState = uiState,
                            onFilterChange = onFilterChange,
                            onQuickTitleChange = onQuickTitleChange,
                            onQuickDescriptionChange = onQuickDescriptionChange,
                            onQuickDueDateChange = onQuickDueDateChange,
                            onQuickDueTimeChange = onQuickDueTimeChange,
                            onQuickPriorityChange = onQuickPriorityChange,
                            onQuickRepeatTypeChange = onQuickRepeatTypeChange,
                            onQuickRepeatWeekdayChange = onQuickRepeatWeekdayChange,
                            onQuickRepeatMonthChange = onQuickRepeatMonthChange,
                            onQuickRepeatDayChange = onQuickRepeatDayChange,
                            onQuickSubmit = onQuickSubmit,
                            onOpenQuickEditor = onOpenQuickEditor,
                            onOpenTodo = onOpenTodo,
                            onDismissEditor = onDismissEditor,
                            onUpdateEditor = onUpdateEditor,
                            onSaveEditor = onSaveEditor,
                            onToggleTodo = onToggleTodo,
                            onDeleteTodo = onDeleteTodo,
                            onRefresh = onRefreshTodos,
                        )
                    }
                    composable(AppRoute.SETTINGS.route) {
                        SettingsPage(
                            uiState = uiState,
                            onSetPlatformEnabled = onSetPlatformEnabled,
                            onCommitPlatformUrl = onCommitPlatformUrl,
                            onCommitApiKey = onCommitApiKey,
                            onSetSyncIntervalSeconds = onSetSyncIntervalSeconds,
                            onSetThemeMode = onSetThemeMode,
                            onSetWidgetThemeMode = onSetWidgetThemeMode,
                            onSetWidgetTransparency = onSetWidgetTransparency,
                            onSetWidgetColorfulTextEnabled = onSetWidgetColorfulTextEnabled,
                            onSetQuickTodoDefaultTodayEnabled = onSetQuickTodoDefaultTodayEnabled,
                            onTestPlatformConnection = onTestPlatformConnection,
                        )
                    }
                    composable(AppRoute.DEVICE_SWITCH.route) {
                        DeviceSwitchPage(
                            uiState = uiState,
                            onRefreshDevices = onRefreshDevices,
                            onSelectDevice = onSelectDevice,
                        )
                    }
                    composable(AppRoute.ABOUT.route) {
                        AboutPage()
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    showDot: Boolean,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label)
                if (showDot) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5A5A)),
                    )
                }
            }
        },
        icon = { Icon(icon, contentDescription = null) },
    )
}

@Composable
private fun AppBackdrop(
    modifier: Modifier = Modifier,
    showDecor: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val backgroundModifier = if (showDecor) {
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
    } else {
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    }
    Box(
        modifier = backgroundModifier,
    ) {
        if (showDecor) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )
        }
        content()
    }
}

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun Int.toPriorityLabel(): String = when (this) {
    2 -> "紧急"
    1 -> "重要"
    else -> "普通"
}

private fun RepeatType?.toRepeatTypeLabel(): String = when (this) {
    null -> "不设置"
    RepeatType.NONE -> "不重复"
    RepeatType.DAILY -> "每天"
    RepeatType.WEEKLY -> "每周"
    RepeatType.MONTHLY -> "每月"
    RepeatType.YEARLY -> "每年"
}

private fun LocalDate.toTodoDueDateLabel(): String {
    val today = LocalDate.now(BeijingZoneId)
    return when (this) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        else -> toString()
    }
}

private fun Todo.dueDateTimeLabel(): String = buildString {
    if (dueDate != null) {
        append(dueDate.toTodoDueDateLabel())
        if (dueTime != null) {
            append(" ")
            append(dueTime)
        }
    } else if (dueTime != null) {
        append(dueTime)
    }
}

private fun Todo.repeatSummaryLabel(): String? = when (repeatType) {
    RepeatType.DAILY -> "每天"
    RepeatType.WEEKLY -> "每周${(repeatWeekday ?: dueDate?.dayOfWeek?.value?.rem(7) ?: 0).toWeekdayLabel()}"
    RepeatType.MONTHLY -> repeatDay?.let { "每月${it}号" } ?: "每月"
    RepeatType.YEARLY -> when {
        repeatMonth != null && repeatDay != null -> "每年${repeatMonth}月${repeatDay}日"
        repeatMonth != null -> "每年${repeatMonth}月"
        repeatDay != null -> "每年${repeatDay}日"
        else -> "每年"
    }

    else -> null
}

private fun Todo.metaSummaryLabel(): String = buildList {
    repeatSummaryLabel()?.let(::add)
    dueDateTimeLabel().takeIf { it.isNotBlank() }?.let(::add)
}.joinToString(" · ")

private fun Int.toPriorityColor(): Color = when (this) {
    2 -> Color(0xFFDC2626)
    1 -> Color(0xFFD97706)
    else -> Color(0xFF475569)
}

private fun LocalDate.toBeijingEpochMillis(): Long =
    atStartOfDay(BeijingZoneId).toInstant().toEpochMilli()

private fun Long.toBeijingLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(BeijingZoneId).toLocalDate()

private fun LocalDate.toDatePickerUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toDatePickerLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun LocalDate?.quickDateLabel(): String {
    val today = LocalDate.now(BeijingZoneId)
    return when (this) {
        null -> "不设置"
        today -> "今天"
        today.plusDays(1) -> "明天"
        else -> toString()
    }
}

private fun Long?.toLastSyncLabel(): String {
    if (this == null) return "暂未同步"
    return Instant.ofEpochMilli(this)
        .atZone(BeijingZoneId)
        .toLocalDateTime()
        .format(LastSyncFormatter)
}

@Composable
private fun TodosPage(
    uiState: AppUiState,
    onFilterChange: (TodoFilter) -> Unit,
    onQuickTitleChange: (String) -> Unit,
    onQuickDescriptionChange: (String) -> Unit,
    onQuickDueDateChange: (LocalDate?) -> Unit,
    onQuickDueTimeChange: (String) -> Unit,
    onQuickPriorityChange: (Int) -> Unit,
    onQuickRepeatTypeChange: (RepeatType?) -> Unit,
    onQuickRepeatWeekdayChange: (Int?) -> Unit,
    onQuickRepeatMonthChange: (Int?) -> Unit,
    onQuickRepeatDayChange: (Int?) -> Unit,
    onQuickSubmit: () -> Unit,
    onOpenQuickEditor: () -> Unit,
    onOpenTodo: (Todo) -> Unit,
    onDismissEditor: () -> Unit,
    onUpdateEditor: ((TodoEditDraft) -> TodoEditDraft) -> Unit,
    onSaveEditor: () -> Unit,
    onToggleTodo: (Todo) -> Unit,
    onDeleteTodo: (Todo) -> Unit,
    onRefresh: () -> Unit,
) {
    var showEditDatePicker by rememberSaveable { mutableStateOf(false) }
    var showQuickDatePicker by rememberSaveable { mutableStateOf(false) }
    var composerFocused by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    val showComposerActions = composerFocused && imeVisible
    val filteredTodos = remember(uiState.todos, uiState.todoFilter) {
        when (uiState.todoFilter) {
            TodoFilter.ALL -> uiState.todos
            TodoFilter.ACTIVE -> uiState.todos.filter { !it.completed }
            TodoFilter.COMPLETED -> uiState.todos.filter { it.completed }
        }
    }

    LaunchedEffect(uiState.todoFilter) {
        listState.scrollToItem(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshingTodos,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                FilterBar(
                    selected = uiState.todoFilter,
                    onSelected = onFilterChange,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 6.dp),
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 0.dp,
                        bottom = if (showComposerActions) 126.dp else 84.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (filteredTodos.isEmpty()) {
                        item {
                            WhiteInfoCard(
                                title = if (uiState.isCloudReady) "还没有待办" else "请先完成平台配置",
                                body = if (uiState.isCloudReady) {
                                    "进入页面、创建、编辑、删除和切换完成状态后，都会自动刷新云端列表。"
                                } else {
                                    "先在设置中心填写平台地址和 API 密钥，再选择设备。"
                                },
                            )
                        }
                    } else {
                        items(
                            items = filteredTodos,
                            key = Todo::localId,
                            contentType = { "todo" },
                        ) { todo ->
                            CloudTodoCard(
                                todo = todo,
                                onOpen = { onOpenTodo(todo) },
                                onToggle = { onToggleTodo(todo) },
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(
                    if (showComposerActions) {
                        Modifier.background(
                            color = PanelBackground.copy(alpha = 0.98f),
                            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            QuickComposerBarModern(
                draft = uiState.quickDraft,
                isExpanded = showComposerActions,
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = if (showComposerActions) 10.dp else 4.dp,
                        bottom = if (showComposerActions) 8.dp else 18.dp,
                    )
                    .navigationBarsPadding()
                    .imePadding(),
                onTitleChange = onQuickTitleChange,
                onDateClick = { showQuickDatePicker = true },
                onExpandedChange = { composerFocused = it },
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

@Composable
private fun FilterBar(
    selected: TodoFilter,
    onSelected: (TodoFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TodoFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.Transparent,
                onClick = { onSelected(filter) },
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) {
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary,
                                    ),
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
                                    ),
                                )
                            },
                            shape = RoundedCornerShape(999.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = if (isSelected) 0f else 0.20f),
                            shape = RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = when (filter) {
                            TodoFilter.ALL -> "全部"
                            TodoFilter.ACTIVE -> "未完成"
                            TodoFilter.COMPLETED -> "已完成"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudTodoCard(
    todo: Todo,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    val titleColor = if (todo.completed) TodoCompletedColor else TodoPendingColor
    val secondaryTextColor = panelTextSecondaryColor()
    val priorityLabel = remember(todo.priority) { todo.priority.toPriorityLabel() }
    val priorityColor = remember(todo.priority) { todo.priority.toPriorityColor() }
    val metaSummary = remember(
        todo.repeatType,
        todo.repeatWeekday,
        todo.repeatMonth,
        todo.repeatDay,
        todo.dueDate,
        todo.dueTime,
    ) {
        todo.metaSummaryLabel()
    }
    WhiteCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (todo.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (todo.completed) "取消完成" else "标记完成",
                        modifier = Modifier.size(18.dp),
                        tint = titleColor,
                    )
                }
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    textDecoration = if (todo.completed) TextDecoration.LineThrough else TextDecoration.None,
                )
            }
            if (todo.description.isNotBlank()) {
                Text(
                    text = todo.description,
                    modifier = Modifier.padding(start = 32.dp, top = 6.dp),
                    color = secondaryTextColor,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val showPriorityDivider = metaSummary.isNotBlank()
                Text(
                    text = priorityLabel,
                    color = priorityColor,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showPriorityDivider) {
                    Text(
                        text = "·",
                        color = secondaryTextColor,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = metaSummary,
                        color = secondaryTextColor,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickComposerBar(
    draft: QuickTodoDraft,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    onTitleChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onMore: () -> Unit,
    onSubmit: () -> Unit,
) {
    val onTimeChange: (String) -> Unit = {}
    var timeMenuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        WhiteCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = draft.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { state -> onExpandedChange(state.isFocused) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = panelTextPrimaryColor()),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                        ) {
                            if (draft.title.isBlank()) {
                                Text(
                                    text = "点击新增待办",
                                    color = PanelTextSecondary,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box {
                    TextButton(
                        onClick = onDateClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = draft.dueTimeText.takeIf { it.isNotBlank() } ?: "时间",
                            maxLines = 1,
                        )
                    }
                    DropdownMenu(
                        expanded = timeMenuExpanded,
                        onDismissRequest = { timeMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            modifier = Modifier.height(44.dp),
                            text = { Text("不设置") },
                            onClick = {
                                timeMenuExpanded = false
                                onTimeChange("")
                            },
                        )
                        TimeOptions.forEach { option ->
                            DropdownMenuItem(
                                modifier = Modifier.height(44.dp),
                                text = { Text(option) },
                                onClick = {
                                    timeMenuExpanded = false
                                    onTimeChange(option)
                                },
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(visible = isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = onCancel) {
                        Text("取消")
                    }
                    TextButton(onClick = onMore) {
                        Text("更多")
                    }
                    Button(onClick = onSubmit) {
                        Text("创建")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickComposerBarModern(
    draft: QuickTodoDraft,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    onTitleChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onMore: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        WhiteCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PanelBackground),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = draft.title,
                        onValueChange = onTitleChange,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { state -> onExpandedChange(state.isFocused) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = panelTextPrimaryColor()),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                            ) {
                                if (draft.title.isBlank()) {
                                    Text(
                                        text = "点击新增待办",
                                        color = panelTextSecondaryColor(),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = onDateClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = draft.dueDate.quickDateLabel(),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                    }
                }
                if (isExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PanelBackground)
                            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        TextButton(onClick = onCancel) {
                            Text("取消")
                        }
                        TextButton(onClick = onMore) {
                            Text("更多")
                        }
                        Button(onClick = onSubmit) {
                            Text("创建")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditTodoSheet(
    draft: TodoEditDraft,
    onDismiss: () -> Unit,
    onUpdate: ((TodoEditDraft) -> TodoEditDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onShowDatePicker: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = PanelBackground,
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .imePadding(),
            containerColor = PanelBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                WhiteCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    shadowElevation = 0.dp,
                    innerPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (draft.isCreateMode || draft.remoteId == null) {
                                    onDismiss()
                                } else {
                                    showDeleteConfirmation = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (draft.isCreateMode || draft.remoteId == null) "取消" else "删除")
                        }
                        Button(
                            onClick = onSave,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("保存")
                        }
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
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFD1D5DB)),
                )
                Text(
                    text = if (draft.isCreateMode) "新增待办" else "编辑待办",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PanelTextPrimary,
                )
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { value -> onUpdate { current -> current.copy(title = value) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    label = { Text("标题") },
                    singleLine = true,
                    colors = whiteOutlinedFieldColors(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CompactActionField(
                        modifier = Modifier.weight(1f),
                        label = "日期",
                        value = draft.dueDate?.toString(),
                        placeholder = "选择日期",
                        onClick = onShowDatePicker,
                    )
                    CompactMenuField(
                        modifier = Modifier.weight(1f),
                        label = "时间",
                        value = draft.dueTimeText.takeIf { it.isNotBlank() },
                        placeholder = "选择时间",
                        options = buildList {
                            add(MenuOption("", "不设置"))
                            addAll(TimeOptions.map { MenuOption(it, it) })
                        },
                        onSelect = { value ->
                            onUpdate { current -> current.copy(dueTimeText = value) }
                        },
                        menuMaxHeight = 240.dp,
                    )
                }
                SheetTitle("优先级")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(top = 10.dp)) {
                    listOf(0, 1, 2).forEachIndexed { index, priority ->
                        SegmentedButton(
                            selected = draft.priority == priority,
                            onClick = { onUpdate { current -> current.copy(priority = priority) } },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                        ) {
                            Text(priority.toPriorityLabel())
                        }
                    }
                }
                SheetTitle("重复类型")
                CompactMenuField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = "重复类型",
                    value = (draft.repeatType ?: RepeatType.NONE).toRepeatTypeLabel(),
                    placeholder = "不重复",
                    options = listOf(
                        MenuOption(RepeatType.NONE, "不重复"),
                        MenuOption(RepeatType.DAILY, "每天"),
                        MenuOption(RepeatType.WEEKLY, "每周"),
                        MenuOption(RepeatType.MONTHLY, "每月"),
                        MenuOption(RepeatType.YEARLY, "每年"),
                    ),
                    onSelect = { value ->
                        onUpdate { current -> current.copy(repeatType = value) }
                    },
                )
                when (draft.repeatType ?: RepeatType.NONE) {
                    RepeatType.WEEKLY -> {
                        CompactMenuField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            label = "周几",
                            value = draft.repeatWeekday?.toWeekdayLabel(),
                            placeholder = "选择周几",
                            options = (0..6).map { MenuOption(it, it.toWeekdayLabel()) },
                            onSelect = { value ->
                                onUpdate { current -> current.copy(repeatWeekday = value) }
                            },
                        )
                    }

                    RepeatType.MONTHLY -> {
                        CompactMenuField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            label = "每月几号",
                            value = draft.repeatDay?.let { "$it 号" },
                            placeholder = "选择日期",
                            options = (1..31).map { MenuOption(it, "$it 号") },
                            onSelect = { value ->
                                onUpdate { current -> current.copy(repeatDay = value) }
                            },
                        )
                    }

                    RepeatType.YEARLY -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CompactMenuField(
                                modifier = Modifier.weight(1f),
                                label = "月份",
                                value = draft.repeatMonth?.let { "$it 月" },
                                placeholder = "选择月份",
                                options = (1..12).map { MenuOption(it, "$it 月") },
                                onSelect = { value ->
                                    onUpdate { current -> current.copy(repeatMonth = value) }
                                },
                            )
                            CompactMenuField(
                                modifier = Modifier.weight(1f),
                                label = "日期",
                                value = draft.repeatDay?.let { "$it 号" },
                                placeholder = "选择日期",
                                options = (1..31).map { MenuOption(it, "$it 号") },
                                onSelect = { value ->
                                    onUpdate { current -> current.copy(repeatDay = value) }
                                },
                            )
                        }
                    }

                    else -> Unit
                }
                SheetTitle("关联信息")
                WhiteCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 0.dp,
                    innerPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    AboutRow("关联设备", draft.deviceName.ifBlank { "未关联设备" })
                    AboutRow("创建时间", draft.createdAtLabel.ifBlank { "创建后生成" })
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("确认删除") },
            text = { Text("删除后将同步删除云端待办，确定继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun SettingsPage(
    uiState: AppUiState,
    onSetPlatformEnabled: (Boolean) -> Unit,
    onCommitPlatformUrl: (String) -> Unit,
    onCommitApiKey: (String) -> Unit,
    onSetSyncIntervalSeconds: (Int) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetWidgetThemeMode: (ThemeMode) -> Unit,
    onSetWidgetTransparency: (Float) -> Unit,
    onSetWidgetColorfulTextEnabled: (Boolean) -> Unit,
    onSetQuickTodoDefaultTodayEnabled: (Boolean) -> Unit,
    onTestPlatformConnection: (String, String) -> Unit,
) {
    var platformUrlText by rememberSaveable(uiState.platformUrl) { mutableStateOf(uiState.platformUrl) }
    var apiKeyText by rememberSaveable(uiState.apiKey) { mutableStateOf(uiState.apiKey) }
    var urlHadFocus by remember { mutableStateOf(false) }
    var apiKeyHadFocus by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val alarmManager = remember(context) { context.getSystemService(AlarmManager::class.java) }
    val exactAlarmSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val exactAlarmGranted = !exactAlarmSupported || alarmManager?.canScheduleExactAlarms() == true

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            WhiteSection {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("接入平台", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "开启后待办页面与设备页面都会直接读取云端接口。",
                            modifier = Modifier.padding(top = 4.dp),
                            color = PanelTextSecondary,
                        )
                    }
                    Switch(
                        checked = uiState.platformEnabled,
                        onCheckedChange = onSetPlatformEnabled,
                    )
                }
            }
        }
        if (uiState.platformEnabled) {
            item {
                WhiteSection {
                    OutlinedTextField(
                        value = platformUrlText,
                        onValueChange = { platformUrlText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { state ->
                                if (urlHadFocus && !state.isFocused) {
                                    onCommitPlatformUrl(platformUrlText)
                                }
                                urlHadFocus = state.isFocused
                            },
                        label = { Text("平台地址") },
                        placeholder = { Text("https://cloud.zectrix.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        colors = whiteOutlinedFieldColors(),
                    )
                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .onFocusChanged { state ->
                                if (apiKeyHadFocus && !state.isFocused) {
                                    onCommitApiKey(apiKeyText)
                                }
                                apiKeyHadFocus = state.isFocused
                            },
                        label = { Text("API 密钥") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = whiteOutlinedFieldColors(),
                    )
                }
            }
            item {
                WhiteSection {
                    SheetTitle("同步周期")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(top = 10.dp)) {
                        listOf(60, 120, 300, 600).forEachIndexed { index, seconds ->
                            SegmentedButton(
                                selected = uiState.syncIntervalSeconds == seconds,
                                onClick = { onSetSyncIntervalSeconds(seconds) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 4),
                            ) {
                                Text("${seconds}s")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("新增待办默认今天", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "开启后底部新增输入框会默认带今天截止日期；关闭则默认不设置日期。",
                                modifier = Modifier.padding(top = 4.dp),
                                color = PanelTextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = uiState.quickTodoDefaultTodayEnabled,
                            onCheckedChange = onSetQuickTodoDefaultTodayEnabled,
                        )
                    }
                    Text(
                        text = "上次同步时间：${uiState.lastSyncAtMillis.toLastSyncLabel()}",
                        modifier = Modifier.padding(top = 12.dp),
                        color = panelTextSecondaryColor(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (exactAlarmSupported) {
                        Text(
                            text = "精确闹钟权限：${if (exactAlarmGranted) "已开启" else "未开启"}",
                            modifier = Modifier.padding(top = 10.dp),
                            color = panelTextSecondaryColor(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (!exactAlarmGranted) {
                            Text(
                                text = "未开启时系统可能将 60s 同步延后到约 70~120s（由系统调度策略决定）。",
                                modifier = Modifier.padding(top = 4.dp),
                                color = panelTextSecondaryColor(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    runCatching { context.startActivity(intent) }
                                        .onFailure {
                                            Toast.makeText(
                                                context,
                                                "当前系统暂不支持直接跳转，请手动到系统设置中开启精确闹钟权限",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                            ) {
                                Text("开启精确闹钟权限")
                            }
                        }
                    }
                    Button(
                        onClick = {
                            focusManager.clearFocus(force = true)
                            onCommitPlatformUrl(platformUrlText)
                            onCommitApiKey(apiKeyText)
                            onTestPlatformConnection(platformUrlText, apiKeyText)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp),
                        enabled = !uiState.isTestingConnection,
                    ) {
                        Text(if (uiState.isTestingConnection) "正在测试连接..." else "测试平台连接")
                    }
                }
            }
        }
        item {
            WhiteSection {
                SheetTitle("App主题样式")
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> "跟随系统"
                                        ThemeMode.LIGHT -> "浅色"
                                        ThemeMode.DARK -> "深色"
                                    },
                                )
                            },
                        )
                    }
                }
                SheetTitle("小组件主题样式")
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.widgetThemeMode == mode,
                            onClick = { onSetWidgetThemeMode(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> "跟随系统"
                                        ThemeMode.LIGHT -> "浅色"
                                        ThemeMode.DARK -> "深色"
                                    },
                                )
                            },
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("小组件彩色文字", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "关闭后文字跟随小组件主题使用黑白色。",
                            modifier = Modifier.padding(top = 4.dp),
                            color = PanelTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = uiState.widgetColorfulTextEnabled,
                        onCheckedChange = onSetWidgetColorfulTextEnabled,
                    )
                }
                SheetTitle("小组件透明度")
                Text(
                    text = "${(uiState.widgetTransparency * 100).toInt()}%",
                    modifier = Modifier.padding(top = 10.dp),
                    color = PanelTextSecondary,
                )
                Slider(
                    value = uiState.widgetTransparency,
                    onValueChange = onSetWidgetTransparency,
                    valueRange = 0f..0.9f,
                )
                Button(
                    onClick = {
                        val pinned = requestPinStickyNoteWidget(context)
                        Toast.makeText(
                            context,
                            if (pinned) "已请求添加小组件，请到桌面确认" else "当前桌面不支持一键添加，请手动添加小组件",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                ) {
                    Text("添加小组件至桌面")
                }
            }
        }
    }
}

@Composable
private fun DeviceSwitchPage(
    uiState: AppUiState,
    onRefreshDevices: () -> Unit,
    onSelectDevice: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            WhiteSection {
                Button(
                    onClick = onRefreshDevices,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isRefreshingDevices,
                ) {
                    Text(if (uiState.isRefreshingDevices) "正在刷新设备..." else "刷新设备列表")
                }
            }
        }
        if (!uiState.isCloudReady) {
            item {
                WhiteInfoCard(
                    title = "还没有完成平台配置",
                    body = "请先到设置中心开启接入平台，并填写正确的平台地址和 API 密钥。",
                )
            }
        } else if (uiState.devices.isEmpty()) {
            item {
                WhiteInfoCard(
                    title = "当前没有拉到设备",
                    body = "点击上方刷新按钮，或先回到设置中心测试平台连接。",
                )
            }
        } else {
            items(uiState.devices, key = Device::deviceId) { device ->
                DeviceCardModern(
                    device = device,
                    isSelected = device.deviceId == uiState.selectedDeviceId,
                    onClick = { onSelectDevice(device.deviceId) },
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: Device,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    WhiteCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(device.alias.ifBlank { device.deviceId }, fontWeight = FontWeight.SemiBold, color = PanelTextPrimary)
                Text(
                    text = buildString {
                        append(device.board.ifBlank { "未知型号" })
                        append(" 路 ")
                        append(device.deviceId)
                    },
                    modifier = Modifier.padding(top = 4.dp),
                    color = PanelTextSecondary,
                )
            }
            if (isSelected) {
                Icon(Icons.Rounded.DoneAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DeviceCardModern(
    device: Device,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    WhiteCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = device.alias.ifBlank { device.deviceId },
                    fontWeight = FontWeight.SemiBold,
                    color = PanelTextPrimary,
                )
                Text(
                    text = device.deviceId,
                    modifier = Modifier.padding(top = 4.dp),
                    color = PanelTextSecondary,
                )
            }
            if (isSelected) {
                Icon(Icons.Rounded.DoneAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun AboutPage() {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            WhiteSection {
                AboutRow("版本", "0.0.17")
                AboutRow("作者", "PASSHEEP")
            }
        }
        item {
            WhiteSection {
                Text("相关链接", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = { uriHandler.openUri("https://cloud.zectrix.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                ) {
                    Text("极趣云平台")
                }
                OutlinedButton(
                    onClick = { uriHandler.openUri("https://wiki.zectrix.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    Text("极趣科技-Wiki")
                }
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = PanelTextSecondary)
        Text(value, fontWeight = FontWeight.Medium, color = PanelTextPrimary)
    }
}

@Composable
private fun isPanelDarkTheme(): Boolean = MaterialTheme.colorScheme.background.red < 0.5f

@Composable
private fun panelTextPrimaryColor(): Color = PanelTextPrimary

@Composable
private fun panelTextSecondaryColor(): Color = PanelTextSecondary

@Composable
private fun WhiteSection(content: @Composable ColumnScope.() -> Unit) {
    WhiteCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp,
        innerPadding = PaddingValues(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

@Composable
private fun WhiteInfoCard(
    title: String,
    body: String,
) {
    WhiteSection {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = PanelTextPrimary)
        Text(
            text = body,
            modifier = Modifier.padding(top = 8.dp),
            color = PanelTextSecondary,
        )
    }
}

@Composable
private fun WhiteCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    shadowElevation: androidx.compose.ui.unit.Dp,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = PanelBackground,
        contentColor = PanelTextPrimary,
        border = androidx.compose.foundation.BorderStroke(1.dp, PanelBorder),
        shadowElevation = shadowElevation,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            content = content,
        )
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 12.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = PanelTextPrimary,
    )
}

@Composable
private fun CompactActionField(
    modifier: Modifier = Modifier,
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = PanelTextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(12.dp),
                color = PanelBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, PanelBorderStrong),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = value ?: placeholder,
                        modifier = Modifier.weight(1f),
                        color = if (value == null) PanelTextSecondary else PanelTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        Icons.Rounded.DateRange,
                        contentDescription = null,
                        tint = PanelTextSecondary,
                    )
                }
            }
            if (onClear != null) {
                TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                    Text("清空")
                }
            }
        }
    }
}

private data class MenuOption<T>(
    val value: T,
    val label: String,
)

@Composable
private fun <T> CompactMenuField(
    modifier: Modifier = Modifier,
    label: String,
    value: String?,
    placeholder: String,
    options: List<MenuOption<T>>,
    onSelect: (T) -> Unit,
    onClear: (() -> Unit)? = null,
    menuMaxHeight: Dp = 280.dp,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            text = label,
            color = PanelTextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    shape = RoundedCornerShape(12.dp),
                    color = PanelBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PanelBorderStrong),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = value ?: placeholder,
                            modifier = Modifier.weight(1f),
                            color = if (value == null) PanelTextSecondary else PanelTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            tint = PanelTextSecondary,
                        )
                    }
                }
                DropdownMenu(
                    modifier = Modifier.heightIn(max = menuMaxHeight),
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            modifier = Modifier.height(44.dp),
                            text = { Text(option.label) },
                            onClick = {
                                expanded = false
                                onSelect(option.value)
                            },
                        )
                    }
                }
            }
            if (onClear != null) {
                TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                    Text("清空")
                }
            }
        }
    }
}

@Composable
private fun whiteOutlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = PanelTextPrimary,
    unfocusedTextColor = PanelTextPrimary,
    disabledTextColor = PanelTextPrimary,
    focusedLabelColor = PanelTextSecondary,
    unfocusedLabelColor = PanelTextSecondary,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedContainerColor = PanelBackground,
    unfocusedContainerColor = PanelBackground,
    disabledContainerColor = PanelBackground,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = PanelBorderStrong,
)

private fun Int.toWeekdayLabel(): String = when (this) {
    0 -> "周日"
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    else -> "周六"
}


