package com.passheep.sticky_note.feature.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.settings.AppAccentTheme
import com.passheep.sticky_note.core.settings.AppBackgroundStyle
import com.passheep.sticky_note.core.settings.DefaultTodoFilter
import com.passheep.sticky_note.core.settings.ThemeMode
import com.passheep.sticky_note.core.settings.WidgetColorTheme
import com.passheep.sticky_note.core.settings.WidgetStylePreset
import java.time.LocalDate

/**
 * 智能便利贴无底栏极简导航外壳：
 * 使用 AnimatedContent 呈现丝滑的横向推入与返回转场动画，告别生硬瞬切。
 */
@Composable
fun StickyNoteAppShell(
    currentRoute: String,
    isForwardNavigation: Boolean,
    pendingScrollTodoId: String?,
    onConsumePendingScroll: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onNavigateBack: () -> Unit,
    uiState: AppUiState,
    onPageEntered: (String) -> Unit,
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
    onRefreshTodos: () -> Unit,
    onSetPlatformEnabled: (Boolean) -> Unit,
    onCommitPlatformUrl: (String) -> Unit,
    onCommitApiKey: (String) -> Unit,
    onSetSyncIntervalSeconds: (Int) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetBackgroundStyle: (AppBackgroundStyle) -> Unit,
    onSetAccentTheme: (AppAccentTheme) -> Unit,
    onSetWidgetThemeMode: (ThemeMode) -> Unit,
    onSetWidgetStylePreset: (WidgetStylePreset) -> Unit,
    onSetWidgetColorTheme: (WidgetColorTheme) -> Unit,
    onSetWidgetTransparency: (Float) -> Unit,
    onSetWidgetColorfulTextEnabled: (Boolean) -> Unit,
    onSetQuickTodoDefaultTodayEnabled: (Boolean) -> Unit,
    onSetDefaultTodoFilter: (DefaultTodoFilter) -> Unit,
    onSetWidgetDefaultFilter: (DefaultTodoFilter) -> Unit,
    onTestPlatformConnection: (String, String) -> Unit,
    onRefreshDevices: () -> Unit,
    onSelectDevice: (String) -> Unit,
) {
    LaunchedEffect(currentRoute) {
        onPageEntered(currentRoute)
    }

    PaperCanvasBackdrop(showSoftGlow = currentRoute == AppRoute.TODOS.route) {
        AnimatedContent(
            targetState = currentRoute,
            transitionSpec = {
                val enterSpec = tween<Int>(durationMillis = 250, easing = FastOutSlowInEasing)
                val exitSpec = tween<Int>(durationMillis = 210, easing = FastOutSlowInEasing)
                if (isForwardNavigation) {
                    (slideInHorizontally(animationSpec = tween(250, easing = FastOutSlowInEasing)) { it / 5 } +
                        fadeIn(animationSpec = tween(220))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(210, easing = FastOutSlowInEasing)) { -it / 10 } +
                            fadeOut(animationSpec = tween(170)))
                } else {
                    (slideInHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { -it / 8 } +
                        fadeIn(animationSpec = tween(200))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { it / 5 } +
                            fadeOut(animationSpec = tween(170)))
                }.also {
                    // 保留引用避免编译器未使用提示
                    enterSpec.hashCode()
                    exitSpec.hashCode()
                }
            },
            label = "pageTransition",
        ) { route ->
            when (route) {
                AppRoute.TODOS.route -> {
                    TodosScreen(
                        uiState = uiState,
                        pendingScrollTodoId = pendingScrollTodoId,
                        onConsumePendingScroll = onConsumePendingScroll,
                        onOpenDevices = { onNavigateTo(AppRoute.DEVICE_SWITCH.route) },
                        onOpenSettings = { onNavigateTo(AppRoute.SETTINGS.route) },
                        onFilterChange = onFilterChange,
                        onQuickTitleChange = onQuickTitleChange,
                        onQuickDueDateChange = onQuickDueDateChange,
                        onQuickSubmit = onQuickSubmit,
                        onOpenQuickEditor = onOpenQuickEditor,
                        onOpenTodo = onOpenTodo,
                        onDismissEditor = onDismissEditor,
                        onUpdateEditor = onUpdateEditor,
                        onSaveEditor = onSaveEditor,
                        onToggleTodo = onToggleTodo,
                        onCopyTodo = onCopyTodo,
                        onDeleteTodo = onDeleteTodo,
                        onRefresh = onRefreshTodos,
                    )
                }

                AppRoute.SETTINGS.route -> {
                    SettingsScreen(
                        uiState = uiState,
                        onBack = onNavigateBack,
                        onOpenAbout = { onNavigateTo(AppRoute.ABOUT.route) },
                        onSetPlatformEnabled = onSetPlatformEnabled,
                        onCommitPlatformUrl = onCommitPlatformUrl,
                        onCommitApiKey = onCommitApiKey,
                        onSetSyncIntervalSeconds = onSetSyncIntervalSeconds,
                        onSetThemeMode = onSetThemeMode,
                        onSetBackgroundStyle = onSetBackgroundStyle,
                        onSetAccentTheme = onSetAccentTheme,
                        onSetWidgetThemeMode = onSetWidgetThemeMode,
                        onSetWidgetStylePreset = onSetWidgetStylePreset,
                        onSetWidgetColorTheme = onSetWidgetColorTheme,
                        onSetWidgetTransparency = onSetWidgetTransparency,
                        onSetWidgetColorfulTextEnabled = onSetWidgetColorfulTextEnabled,
                        onSetQuickTodoDefaultTodayEnabled = onSetQuickTodoDefaultTodayEnabled,
                        onSetDefaultTodoFilter = onSetDefaultTodoFilter,
                        onSetWidgetDefaultFilter = onSetWidgetDefaultFilter,
                        onTestPlatformConnection = onTestPlatformConnection,
                    )
                }

                AppRoute.DEVICE_SWITCH.route -> {
                    DeviceSwitchScreen(
                        uiState = uiState,
                        onBack = onNavigateBack,
                        onOpenSettings = { onNavigateTo(AppRoute.SETTINGS.route) },
                        onRefreshDevices = onRefreshDevices,
                        onSelectDevice = onSelectDevice,
                    )
                }

                AppRoute.ABOUT.route -> {
                    AboutScreen(
                        onBack = onNavigateBack,
                    )
                }
            }
        }
    }
}
