package com.passheep.sticky_note

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passheep.sticky_note.core.settings.ThemeMode
import com.passheep.sticky_note.feature.app.AppRoute
import com.passheep.sticky_note.feature.app.MainEvent
import com.passheep.sticky_note.feature.app.StickyNoteAppShell
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val activityViewModel: MainViewModel by viewModels()
    private var intentVersion by mutableIntStateOf(0)

    companion object {
        const val EXTRA_OPEN_TODO_ID = "open_todo_id"
        const val EXTRA_OPEN_SETTINGS = "open_settings"
        const val EXTRA_OPEN_QUICK_EDITOR = "open_quick_editor"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyHighestRefreshRatePreference()
        enableEdgeToEdge()

        setContent {
            val currentIntentVersion = intentVersion
            val viewModel = activityViewModel
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            // 轻量显式路由栈，配合 AnimatedContent 实现 120Hz 丝滑推入/返回转场动画
            val routeStack = remember { mutableStateListOf(AppRoute.TODOS.route) }
            val currentRoute = routeStack.lastOrNull() ?: AppRoute.TODOS.route
            var isForwardNavigation by remember { mutableStateOf(true) }
            var pendingScrollTodoId by remember { mutableStateOf<String?>(null) }

            fun pushRoute(route: String) {
                if (route == currentRoute) return
                if (route == AppRoute.TODOS.route) {
                    isForwardNavigation = false
                    routeStack.clear()
                    routeStack.add(AppRoute.TODOS.route)
                    return
                }
                val existingIndex = routeStack.indexOf(route)
                if (existingIndex >= 0) {
                    isForwardNavigation = false
                    while (routeStack.size > existingIndex + 1) {
                        routeStack.removeAt(routeStack.lastIndex)
                    }
                } else {
                    isForwardNavigation = true
                    routeStack.add(route)
                }
            }

            fun popRoute() {
                if (routeStack.size > 1) {
                    isForwardNavigation = false
                    routeStack.removeAt(routeStack.lastIndex)
                }
            }

            BackHandler(enabled = routeStack.size > 1) {
                popRoute()
            }

            val requestedTodoId = intent?.getStringExtra(EXTRA_OPEN_TODO_ID)
            val shouldOpenSettings = intent?.getBooleanExtra(EXTRA_OPEN_SETTINGS, false) == true
            val shouldOpenQuickEditor = intent?.getBooleanExtra(EXTRA_OPEN_QUICK_EDITOR, false) == true
            val useDarkSystemBars = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (useDarkSystemBars) {
                        SystemBarStyle.dark(Color.Transparent.toArgb())
                    } else {
                        SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
                    },
                    navigationBarStyle = if (useDarkSystemBars) {
                        SystemBarStyle.dark(Color.Transparent.toArgb())
                    } else {
                        SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
                    },
                )
            }

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is MainEvent.Toast -> {
                            Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_SHORT).show()
                        }

                        is MainEvent.Navigate -> {
                            pushRoute(event.route)
                        }

                        is MainEvent.ScrollToTodo -> {
                            pendingScrollTodoId = event.localId
                        }
                    }
                }
            }

            LaunchedEffect(currentIntentVersion, shouldOpenSettings) {
                if (shouldOpenSettings) {
                    pushRoute(AppRoute.SETTINGS.route)
                    intent?.removeExtra(EXTRA_OPEN_SETTINGS)
                }
            }

            LaunchedEffect(currentIntentVersion, shouldOpenQuickEditor) {
                if (shouldOpenQuickEditor) {
                    pushRoute(AppRoute.TODOS.route)
                    viewModel.openQuickEditor()
                    intent?.removeExtra(EXTRA_OPEN_QUICK_EDITOR)
                }
            }

            LaunchedEffect(currentIntentVersion, requestedTodoId) {
                requestedTodoId?.let {
                    pushRoute(AppRoute.TODOS.route)
                    viewModel.openTodoByLocalId(it)
                    intent?.removeExtra(EXTRA_OPEN_TODO_ID)
                }
            }

            StickyNoteTheme(
                themeMode = uiState.themeMode,
                backgroundStyle = uiState.backgroundStyle,
                accentTheme = uiState.accentTheme,
            ) {
                StickyNoteAppShell(
                    currentRoute = currentRoute,
                    isForwardNavigation = isForwardNavigation,
                    pendingScrollTodoId = pendingScrollTodoId,
                    onConsumePendingScroll = { pendingScrollTodoId = null },
                    onNavigateTo = ::pushRoute,
                    onNavigateBack = ::popRoute,
                    uiState = uiState,
                    onPageEntered = viewModel::onPageEntered,
                    onFilterChange = viewModel::updateFilter,
                    onQuickTitleChange = viewModel::updateQuickTitle,
                    onQuickDueDateChange = viewModel::updateQuickDueDate,
                    onQuickSubmit = viewModel::submitQuickTodo,
                    onOpenQuickEditor = viewModel::openQuickEditor,
                    onOpenTodo = viewModel::openTodoEditor,
                    onDismissEditor = viewModel::dismissTodoEditor,
                    onUpdateEditor = viewModel::updateTodoEditor,
                    onSaveEditor = viewModel::saveTodoEditor,
                    onToggleTodo = viewModel::toggleTodo,
                    onCopyTodo = viewModel::copyTodo,
                    onDeleteTodo = viewModel::deleteTodo,
                    onRefreshTodos = viewModel::refreshTodosManually,
                    onSetPlatformEnabled = viewModel::setPlatformEnabled,
                    onCommitPlatformUrl = viewModel::commitPlatformUrl,
                    onCommitApiKey = viewModel::commitApiKey,
                    onSetSyncIntervalSeconds = viewModel::setSyncIntervalSeconds,
                    onSetThemeMode = viewModel::setThemeMode,
                    onSetBackgroundStyle = viewModel::setBackgroundStyle,
                    onSetAccentTheme = viewModel::setAccentTheme,
                    onSetWidgetThemeMode = viewModel::setWidgetThemeMode,
                    onSetWidgetStylePreset = viewModel::setWidgetStylePreset,
                    onSetWidgetColorTheme = viewModel::setWidgetColorTheme,
                    onSetWidgetTransparency = viewModel::setWidgetTransparency,
                    onSetWidgetColorfulTextEnabled = viewModel::setWidgetColorfulTextEnabled,
                    onSetQuickTodoDefaultTodayEnabled = viewModel::setQuickTodoDefaultTodayEnabled,
                    onSetDefaultTodoFilter = viewModel::setDefaultTodoFilter,
                    onSetWidgetDefaultFilter = viewModel::setWidgetDefaultFilter,
                    onTestPlatformConnection = viewModel::testPlatformConnection,
                    onRefreshDevices = viewModel::refreshDevicesManually,
                    onSelectDevice = viewModel::selectDevice,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyHighestRefreshRatePreference()
        activityViewModel.onPageEntered(AppRoute.TODOS.route)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentVersion++
    }

    private fun applyHighestRefreshRatePreference() {
        val displayRef = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        } ?: return
        val currentMode = displayRef.mode
        // 优先在当前物理分辨率下选择最高刷新率模式，避免跨分辨率 modeId 被系统忽略回落 60Hz
        val bestMode = displayRef.supportedModes
            .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
            .maxByOrNull { it.refreshRate }
            ?: displayRef.supportedModes.maxByOrNull { it.refreshRate }
            ?: return
        if (bestMode.refreshRate <= 0f) return
        val params = window.attributes
        val shouldUpdateRate = params.preferredRefreshRate < bestMode.refreshRate - 0.5f
        val shouldUpdateMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            params.preferredDisplayModeId != bestMode.modeId
        if (!shouldUpdateRate && !shouldUpdateMode) return
        params.preferredRefreshRate = bestMode.refreshRate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            params.preferredDisplayModeId = bestMode.modeId
        }
        window.attributes = params
    }
}
