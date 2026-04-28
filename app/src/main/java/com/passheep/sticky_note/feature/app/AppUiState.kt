package com.passheep.sticky_note.feature.app

import com.passheep.sticky_note.core.model.Device
import com.passheep.sticky_note.core.model.RepeatType
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.settings.DEFAULT_PLATFORM_URL
import com.passheep.sticky_note.core.settings.ThemeMode
import java.time.LocalDate
import java.time.ZoneId

private val AppBeijingZoneId: ZoneId = ZoneId.of("Asia/Shanghai")

enum class AppRoute(
    val route: String,
    val title: String,
) {
    TODOS("todos", "待办清单"),
    SETTINGS("settings", "设置中心"),
    ABOUT("about", "关于软件"),
    DEVICE_SWITCH("device_switch", "切换设备"),
}

enum class TodoFilter {
    ALL,
    ACTIVE,
    COMPLETED,
}

data class QuickTodoDraft(
    val title: String = "",
    val description: String = "",
    val dueDate: LocalDate? = null,
    val dueTimeText: String = "",
    val priority: Int = 0,
    val repeatType: RepeatType? = RepeatType.NONE,
    val repeatWeekday: Int? = null,
    val repeatMonth: Int? = null,
    val repeatDay: Int? = null,
)

data class TodoEditDraft(
    val localId: String? = null,
    val remoteId: Long? = null,
    val title: String = "",
    val description: String = "",
    val dueDate: LocalDate? = LocalDate.now(AppBeijingZoneId),
    val dueTimeText: String = "",
    val priority: Int = 0,
    val repeatType: RepeatType? = RepeatType.NONE,
    val repeatWeekday: Int? = null,
    val repeatMonth: Int? = null,
    val repeatDay: Int? = null,
    val deviceName: String = "",
    val createdAtLabel: String = "",
    val isCreateMode: Boolean = false,
    val isFromQuickComposer: Boolean = false,
)

data class AppUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val widgetThemeMode: ThemeMode = ThemeMode.SYSTEM,
    val platformEnabled: Boolean = false,
    val platformUrl: String = DEFAULT_PLATFORM_URL,
    val apiKey: String = "",
    val syncIntervalSeconds: Int = 300,
    val lastSyncAtMillis: Long? = null,
    val selectedDeviceId: String? = null,
    val selectedDeviceName: String? = null,
    val widgetTransparency: Float = 0.18f,
    val widgetColorfulTextEnabled: Boolean = true,
    val quickTodoDefaultTodayEnabled: Boolean = false,
    val todos: List<Todo> = emptyList(),
    val devices: List<Device> = emptyList(),
    val todoFilter: TodoFilter = TodoFilter.ALL,
    val isRefreshingTodos: Boolean = false,
    val isRefreshingDevices: Boolean = false,
    val isTestingConnection: Boolean = false,
    val quickDraft: QuickTodoDraft = QuickTodoDraft(),
    val editDraft: TodoEditDraft? = null,
) {
    val filteredTodos: List<Todo>
        get() = when (todoFilter) {
            TodoFilter.ALL -> todos
            TodoFilter.ACTIVE -> todos.filter { !it.completed }
            TodoFilter.COMPLETED -> todos.filter { it.completed }
        }

    val selectedDeviceLabel: String?
        get() = devices.firstOrNull { it.deviceId == selectedDeviceId }?.alias
            ?: selectedDeviceName
            ?: selectedDeviceId

    val isCloudReady: Boolean
        get() = platformEnabled && platformUrl.isNotBlank() && apiKey.isNotBlank()

    val showSettingsBadge: Boolean
        get() = apiKey.isBlank()
}

sealed interface MainEvent {
    data class Toast(val message: String) : MainEvent
    data class Navigate(val route: String) : MainEvent
}
