package com.passheep.sticky_note

import android.os.Bundle
import android.os.Build
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.passheep.sticky_note.feature.app.AppRoute
import com.passheep.sticky_note.feature.app.MainEvent
import com.passheep.sticky_note.feature.app.StickyNoteAppShell
import com.passheep.sticky_note.core.settings.ThemeMode
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
            val navController = rememberNavController()
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                ?: AppRoute.TODOS.route
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
                            navController.navigateTopLevel(event.route)
                        }
                    }
                }
            }

            LaunchedEffect(currentIntentVersion, shouldOpenSettings) {
                if (shouldOpenSettings) {
                    navController.navigateTopLevel(AppRoute.SETTINGS.route)
                    intent?.removeExtra(EXTRA_OPEN_SETTINGS)
                }
            }

            LaunchedEffect(currentIntentVersion, shouldOpenQuickEditor) {
                if (shouldOpenQuickEditor) {
                    navController.navigateTopLevel(AppRoute.TODOS.route)
                    viewModel.openQuickEditor()
                    intent?.removeExtra(EXTRA_OPEN_QUICK_EDITOR)
                }
            }

            LaunchedEffect(currentIntentVersion, requestedTodoId) {
                requestedTodoId?.let {
                    navController.navigateTopLevel(AppRoute.TODOS.route)
                    viewModel.openTodoByLocalId(it)
                    intent?.removeExtra(EXTRA_OPEN_TODO_ID)
                }
            }

            StickyNoteTheme(themeMode = uiState.themeMode) {
                StickyNoteAppShell(
                    navController = navController,
                    currentRoute = currentRoute,
                    uiState = uiState,
                    onPageEntered = viewModel::onPageEntered,
                    onFilterChange = viewModel::updateFilter,
                    onQuickTitleChange = viewModel::updateQuickTitle,
                    onQuickDescriptionChange = viewModel::updateQuickDescription,
                    onQuickDueDateChange = viewModel::updateQuickDueDate,
                    onQuickDueTimeChange = viewModel::updateQuickDueTimeText,
                    onQuickPriorityChange = viewModel::updateQuickPriority,
                    onQuickRepeatTypeChange = viewModel::updateQuickRepeatType,
                    onQuickRepeatWeekdayChange = viewModel::updateQuickRepeatWeekday,
                    onQuickRepeatMonthChange = viewModel::updateQuickRepeatMonth,
                    onQuickRepeatDayChange = viewModel::updateQuickRepeatDay,
                    onQuickSubmit = viewModel::submitQuickTodo,
                    onOpenQuickEditor = viewModel::openQuickEditor,
                    onOpenTodo = viewModel::openTodoEditor,
                    onDismissEditor = viewModel::dismissTodoEditor,
                    onUpdateEditor = viewModel::updateTodoEditor,
                    onSaveEditor = viewModel::saveTodoEditor,
                    onToggleTodo = viewModel::toggleTodo,
                    onDeleteTodo = viewModel::deleteTodo,
                    onRefreshTodos = viewModel::refreshTodosManually,
                    onSetPlatformEnabled = viewModel::setPlatformEnabled,
                    onCommitPlatformUrl = viewModel::commitPlatformUrl,
                    onCommitApiKey = viewModel::commitApiKey,
                    onSetSyncIntervalSeconds = viewModel::setSyncIntervalSeconds,
                    onSetThemeMode = viewModel::setThemeMode,
                    onSetWidgetThemeMode = viewModel::setWidgetThemeMode,
                    onSetWidgetTransparency = viewModel::setWidgetTransparency,
                    onSetWidgetColorfulTextEnabled = viewModel::setWidgetColorfulTextEnabled,
                    onSetQuickTodoDefaultTodayEnabled = viewModel::setQuickTodoDefaultTodayEnabled,
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
        val bestMode = displayRef.supportedModes.maxByOrNull { it.refreshRate } ?: return
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

private fun androidx.navigation.NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
    }
}
