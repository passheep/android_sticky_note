package com.passheep.sticky_note

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.passheep.sticky_note.core.model.RepeatType
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.model.TodoDraftInput
import com.passheep.sticky_note.core.settings.AppSettings
import com.passheep.sticky_note.core.settings.DEFAULT_PLATFORM_URL
import com.passheep.sticky_note.core.settings.SettingsRepository
import com.passheep.sticky_note.core.settings.ThemeMode
import com.passheep.sticky_note.data.cloud.CloudTodoStore
import com.passheep.sticky_note.data.sync.SyncScheduler
import com.passheep.sticky_note.feature.app.AppRoute
import com.passheep.sticky_note.feature.app.AppUiState
import com.passheep.sticky_note.feature.app.MainEvent
import com.passheep.sticky_note.feature.app.QuickTodoDraft
import com.passheep.sticky_note.feature.app.TodoEditDraft
import com.passheep.sticky_note.feature.app.TodoFilter
import com.passheep.sticky_note.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val cloudTodoStore: CloudTodoStore,
    private val syncScheduler: SyncScheduler,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    private val todoFilter = MutableStateFlow(TodoFilter.ALL)
    private val quickDraft = MutableStateFlow(QuickTodoDraft())
    private val editDraft = MutableStateFlow<TodoEditDraft?>(null)
    private val refreshingTodos = MutableStateFlow(false)
    private val refreshingDevices = MutableStateFlow(false)
    private val testingConnection = MutableStateFlow(false)

    private val mutableEvents = MutableSharedFlow<MainEvent>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events = mutableEvents.asSharedFlow()

    private val baseState: Flow<PartialUiState> = combine(
        settingsRepository.settings,
        cloudTodoStore.todos,
        cloudTodoStore.devices,
        todoFilter,
        refreshingTodos,
    ) { settings, todos, devices, filter, isRefreshingTodos ->
        PartialUiState(
            settings = settings,
            todos = todos,
            devices = devices,
            filter = filter,
            isRefreshingTodos = isRefreshingTodos,
        )
    }

    private val combinedState: Flow<AppUiState> = baseState
        .combine(refreshingDevices) { partial, isRefreshingDevices ->
            partial to isRefreshingDevices
        }
        .combine(testingConnection) { (partial, isRefreshingDevices), isTesting ->
            Triple(partial, isRefreshingDevices, isTesting)
        }
        .combine(quickDraft) { triple, quick ->
            triple to quick
        }
        .combine(editDraft) { (triple, quick), editor ->
            val (partial, isRefreshingDevices, isTesting) = triple
            AppUiState(
                themeMode = partial.settings.themeMode,
                widgetThemeMode = partial.settings.widgetThemeMode,
                platformEnabled = partial.settings.platformEnabled,
                platformUrl = partial.settings.platformUrl,
                apiKey = partial.settings.apiKey,
                syncIntervalSeconds = partial.settings.syncIntervalSeconds,
                lastSyncAtMillis = partial.settings.lastSyncAtMillis,
                selectedDeviceId = partial.settings.selectedDeviceId,
                selectedDeviceName = partial.settings.selectedDeviceName,
                widgetTransparency = partial.settings.widgetTransparency,
                widgetColorfulTextEnabled = partial.settings.widgetColorfulTextEnabled,
                quickTodoDefaultTodayEnabled = partial.settings.quickTodoDefaultTodayEnabled,
                todos = partial.todos,
                devices = partial.devices,
                todoFilter = partial.filter,
                isRefreshingTodos = partial.isRefreshingTodos,
                isRefreshingDevices = isRefreshingDevices,
                isTestingConnection = isTesting,
                quickDraft = quick,
                editDraft = editor,
            )
        }

    val uiState: StateFlow<AppUiState> = combinedState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(),
    )

    init {
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.quickTodoDefaultTodayEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    quickDraft.update { current ->
                        applyQuickDefaultDueDateIfSafe(
                            draft = current,
                            defaultTodayEnabled = enabled,
                        )
                    }
                }
        }
    }

    fun onPageEntered(route: String) {
        Log.d(TAG, "onPageEntered route=$route")
        when (route) {
            AppRoute.TODOS.route -> refreshTodos(
                reason = "enter_todos",
                showConfigToast = false,
                showFailureToast = true,
            )

            AppRoute.DEVICE_SWITCH.route -> {
                viewModelScope.launch {
                    if (settingsRepository.settings.first().hasCloudConfig()) {
                        refreshDevicesInternal(
                            reason = "enter_device_switch",
                            showConfigToast = true,
                            showFailureToast = true,
                        )
                    } else {
                        emitToast("请先在设置中心完成平台配置")
                    }
                }
            }
        }
    }

    fun updateFilter(filter: TodoFilter) {
        todoFilter.value = filter
    }

    fun updateQuickTitle(value: String) {
        quickDraft.update { it.copy(title = value) }
    }

    fun updateQuickDescription(value: String) {
        quickDraft.update { it.copy(description = value) }
    }

    fun updateQuickDueDate(value: LocalDate?) {
        quickDraft.update { current ->
            current.copy(
                dueDate = value,
                repeatMonth = if (current.repeatType == RepeatType.YEARLY) value?.monthValue ?: current.repeatMonth else current.repeatMonth,
                repeatDay = when (current.repeatType) {
                    RepeatType.MONTHLY, RepeatType.YEARLY -> value?.dayOfMonth ?: current.repeatDay
                    else -> current.repeatDay
                },
                repeatWeekday = if (current.repeatType == RepeatType.WEEKLY) value?.toWeekdayValue() ?: current.repeatWeekday else current.repeatWeekday,
            )
        }
    }

    fun updateQuickDueTimeText(value: String) {
        quickDraft.update { it.copy(dueTimeText = value) }
    }

    fun updateQuickPriority(value: Int) {
        quickDraft.update { it.copy(priority = value.coerceIn(0, 2)) }
    }

    fun updateQuickRepeatType(value: RepeatType?) {
        quickDraft.update { current -> current.withRepeatType(value) }
    }

    fun updateQuickRepeatWeekday(value: Int?) {
        quickDraft.update { it.copy(repeatWeekday = value?.coerceIn(0, 6)) }
    }

    fun updateQuickRepeatMonth(value: Int?) {
        quickDraft.update { it.copy(repeatMonth = value?.coerceIn(1, 12)) }
    }

    fun updateQuickRepeatDay(value: Int?) {
        quickDraft.update { it.copy(repeatDay = value?.coerceIn(1, 31)) }
    }

    fun submitQuickTodo() {
        val draft = quickDraft.value
        val input = runCatching { draft.toTodoDraftInput() }.getOrElse { error ->
            emitToast(error.message ?: "请先填写完整的待办信息")
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "submitQuickTodo title=${draft.title}")
            runCatching { cloudTodoStore.createTodo(input) }
                .onSuccess {
                    val settings = settingsRepository.settings.first()
                    quickDraft.value = defaultQuickTodoDraft(settings.quickTodoDefaultTodayEnabled)
                    emitToast("已创建待办")
                }
                .onFailure { error ->
                    Log.e(TAG, "submitQuickTodo failed", error)
                    emitToast(error.toUserMessage("新增失败，请检查网络或密钥"))
                }
        }
    }

    fun openQuickEditor() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            editDraft.value = quickDraft.value.toEditorDraft(
                deviceName = settings.selectedDeviceName ?: settings.selectedDeviceId.orEmpty(),
            )
            Log.d(TAG, "openQuickEditor")
        }
    }

    fun openTodoEditor(todo: Todo) {
        editDraft.value = todo.toEditorDraft()
        Log.d(TAG, "openTodoEditor localId=${todo.localId} remoteId=${todo.remoteId}")
    }

    fun openTodoByLocalId(localId: String) {
        viewModelScope.launch {
            val existing = cloudTodoStore.todos.value.firstOrNull { it.localId == localId }
            if (existing != null) {
                openTodoEditor(existing)
                return@launch
            }
            refreshTodosInternal(
                reason = "open_todo",
                showConfigToast = false,
                showFailureToast = false,
            )
            cloudTodoStore.todos.value.firstOrNull { it.localId == localId }?.let(::openTodoEditor)
        }
    }

    fun dismissTodoEditor() {
        editDraft.value = null
    }

    fun updateTodoEditor(transform: (TodoEditDraft) -> TodoEditDraft) {
        editDraft.update { current -> current?.let(transform)?.normalized() }
    }

    fun saveTodoEditor() {
        val draft = editDraft.value ?: return
        val input = runCatching { draft.toTodoDraftInput() }.getOrElse { error ->
            emitToast(error.message ?: "待办内容格式不正确")
            return
        }
        viewModelScope.launch {
            val isCreateMode = draft.isCreateMode || draft.remoteId == null
            Log.d(TAG, "saveTodoEditor create=$isCreateMode remoteId=${draft.remoteId}")
            runCatching {
                if (isCreateMode) {
                    cloudTodoStore.createTodo(input)
                } else {
                    cloudTodoStore.updateTodo(requireNotNull(draft.remoteId), input)
                }
            }.onSuccess {
                editDraft.value = null
                if (draft.isFromQuickComposer) {
                    val settings = settingsRepository.settings.first()
                    quickDraft.value = defaultQuickTodoDraft(settings.quickTodoDefaultTodayEnabled)
                }
                emitToast(if (isCreateMode) "已创建待办" else "已保存修改")
            }.onFailure { error ->
                Log.e(TAG, "saveTodoEditor failed remoteId=${draft.remoteId}", error)
                emitToast(error.toUserMessage(if (isCreateMode) "创建失败，请检查网络或密钥" else "保存失败，请检查网络或密钥"))
            }
        }
    }

    fun toggleTodo(todo: Todo) {
        val remoteId = todo.remoteId ?: return
        viewModelScope.launch {
            Log.d(TAG, "toggleTodo remoteId=$remoteId")
            runCatching { cloudTodoStore.toggleCompleted(remoteId) }
                .onFailure { error ->
                    Log.e(TAG, "toggleTodo failed remoteId=$remoteId", error)
                    emitToast(error.toUserMessage("状态切换失败"))
                }
        }
    }

    fun deleteTodo(todo: Todo) {
        val remoteId = todo.remoteId ?: return
        viewModelScope.launch {
            Log.d(TAG, "deleteTodo remoteId=$remoteId")
            runCatching { cloudTodoStore.deleteTodo(remoteId) }
                .onSuccess {
                    if (editDraft.value?.remoteId == remoteId) {
                        editDraft.value = null
                    }
                    emitToast("已删除待办")
                }
                .onFailure { error ->
                    Log.e(TAG, "deleteTodo failed remoteId=$remoteId", error)
                    emitToast(error.toUserMessage("删除失败"))
                }
        }
    }

    fun refreshTodosManually() {
        refreshTodos(reason = "manual_refresh", showConfigToast = true, showFailureToast = true)
    }

    fun refreshDevicesManually() {
        refreshDevices(reason = "manual_refresh_devices", showConfigToast = true, showFailureToast = true)
    }

    fun setPlatformEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePlatformEnabled(value)
            if (value) {
                syncScheduler.scheduleRecurringSync(settingsRepository.settings.first().syncIntervalSeconds)
            } else {
                syncScheduler.cancelRecurringSync()
                cloudTodoStore.clearCloudState()
            }
            widgetUpdater.refreshAllAsync()
        }
    }

    fun commitPlatformUrl(value: String) {
        val normalized = value.normalizePlatformUrlOrDefault()
        if (normalized.toHttpUrlOrNull() == null) {
            emitToast("平台地址格式不正确，请输入完整的 http 或 https 地址")
            return
        }
        viewModelScope.launch {
            settingsRepository.updatePlatformUrl(normalized)
            widgetUpdater.refreshAll()
        }
    }

    fun commitApiKey(value: String) {
        viewModelScope.launch {
            settingsRepository.updateApiKey(value.trim())
            widgetUpdater.refreshAll()
        }
    }

    fun setSyncIntervalSeconds(value: Int) {
        val safeValue = value.coerceIn(60, 600)
        viewModelScope.launch {
            settingsRepository.updateSyncIntervalSeconds(safeValue)
            if (settingsRepository.settings.first().platformEnabled) {
                syncScheduler.scheduleRecurringSync(safeValue)
            }
        }
    }

    fun setThemeMode(value: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(value)
        }
    }

    fun setWidgetThemeMode(value: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateWidgetThemeMode(value)
            widgetUpdater.refreshAll()
        }
    }

    fun setWidgetTransparency(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateWidgetTransparency(value.coerceIn(0f, 0.9f))
            widgetUpdater.refreshAll()
        }
    }

    fun setWidgetColorfulTextEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWidgetColorfulTextEnabled(value)
            widgetUpdater.refreshAll()
        }
    }

    fun setQuickTodoDefaultTodayEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateQuickTodoDefaultTodayEnabled(value)
            quickDraft.update { current ->
                applyQuickDefaultDueDateIfSafe(
                    draft = current,
                    defaultTodayEnabled = value,
                )
            }
        }
    }

    fun testPlatformConnection(platformUrl: String, apiKey: String) {
        val normalizedUrl = platformUrl.normalizePlatformUrlOrDefault()
        if (normalizedUrl.toHttpUrlOrNull() == null) {
            emitToast("平台地址格式不正确，请输入完整的 http 或 https 地址")
            return
        }
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (!settings.platformEnabled) {
                emitToast("请先开启接入平台")
                return@launch
            }
            testingConnection.value = true
            runCatching {
                settingsRepository.updatePlatformUrl(normalizedUrl)
                settingsRepository.updateApiKey(apiKey.trim())
                cloudTodoStore.refreshDevices(reason = "test_connection", clearOnFailure = true)
            }.onSuccess { devices ->
                persistSelectedDeviceAlias(
                    selectedDeviceId = settingsRepository.settings.first().selectedDeviceId,
                    devices = devices,
                )
                emitToast("连接成功，正在切换到设备页面")
                emitNavigate(AppRoute.DEVICE_SWITCH.route)
            }.onFailure { error ->
                Log.e(TAG, "testPlatformConnection failed", error)
                emitToast("同步失败，请检查密钥是否正确")
            }
            testingConnection.value = false
        }
    }

    fun selectDevice(deviceId: String) {
        viewModelScope.launch {
            runCatching {
                val selectedDeviceName = cloudTodoStore.devices.value
                    .firstOrNull { it.deviceId == deviceId }
                    ?.alias
                    ?: deviceId
                settingsRepository.updateSelectedDeviceId(deviceId)
                settingsRepository.updateSelectedDeviceName(selectedDeviceName)
                refreshDevicesInternal(
                    reason = "select_device",
                    showConfigToast = false,
                    showFailureToast = false,
                )
                refreshTodosInternal(
                    reason = "select_device",
                    showConfigToast = false,
                    showFailureToast = true,
                )
            }.onSuccess {
                emitNavigate(AppRoute.TODOS.route)
                emitToast("已切换设备")
                widgetUpdater.refreshAllAsync()
            }.onFailure { error ->
                Log.e(TAG, "selectDevice failed deviceId=$deviceId", error)
                emitToast(error.toUserMessage("切换设备失败"))
            }
        }
    }

    private fun refreshTodos(
        reason: String,
        showConfigToast: Boolean,
        showFailureToast: Boolean,
    ) {
        viewModelScope.launch {
            refreshTodosInternal(reason, showConfigToast, showFailureToast)
        }
    }

    private fun refreshDevices(
        reason: String,
        showConfigToast: Boolean,
        showFailureToast: Boolean,
    ) {
        viewModelScope.launch {
            refreshDevicesInternal(reason, showConfigToast, showFailureToast)
        }
    }

    private suspend fun refreshTodosInternal(
        reason: String,
        showConfigToast: Boolean,
        showFailureToast: Boolean,
    ) {
        val settings = settingsRepository.settings.first()
        if (!settings.hasCloudConfig()) {
            cloudTodoStore.clearCloudState()
            if (showConfigToast) {
                emitToast("请先在设置中心填写平台地址和 API 密钥")
            }
            return
        }
        refreshingTodos.value = true
        runCatching { cloudTodoStore.refreshTodos(reason = reason, clearOnFailure = true) }
            .onFailure { error ->
                Log.e(TAG, "refreshTodosInternal failed reason=$reason", error)
                if (showFailureToast) {
                    emitToast(error.toUserMessage("同步失败，请检查网络或密钥"))
                }
            }
        refreshingTodos.value = false
    }

    private suspend fun refreshDevicesInternal(
        reason: String,
        showConfigToast: Boolean,
        showFailureToast: Boolean,
    ) {
        val settings = settingsRepository.settings.first()
        if (!settings.hasCloudConfig()) {
            cloudTodoStore.clearCloudState()
            if (showConfigToast) {
                emitToast("请先在设置中心填写平台地址和 API 密钥")
            }
            return
        }
        refreshingDevices.value = true
        runCatching { cloudTodoStore.refreshDevices(reason = reason, clearOnFailure = true) }
            .onSuccess { devices ->
                persistSelectedDeviceAlias(settings.selectedDeviceId, devices)
            }
            .onFailure { error ->
                Log.e(TAG, "refreshDevicesInternal failed reason=$reason", error)
                if (showFailureToast) {
                    emitToast(error.toUserMessage("获取设备失败，请检查网络或密钥"))
                }
            }
        refreshingDevices.value = false
    }

    private suspend fun persistSelectedDeviceAlias(
        selectedDeviceId: String?,
        devices: List<com.passheep.sticky_note.core.model.Device>,
    ) {
        if (selectedDeviceId.isNullOrBlank()) {
            return
        }
        devices.firstOrNull { it.deviceId == selectedDeviceId }?.alias?.let { alias ->
            settingsRepository.updateSelectedDeviceName(alias)
        }
    }

    private fun emitToast(message: String) {
        mutableEvents.tryEmit(MainEvent.Toast(message))
    }

    private fun emitNavigate(route: String) {
        mutableEvents.tryEmit(MainEvent.Navigate(route))
    }

    private companion object {
        const val TAG = "StickyNoteVM"
    }
}

private data class PartialUiState(
    val settings: AppSettings,
    val todos: List<Todo>,
    val devices: List<com.passheep.sticky_note.core.model.Device>,
    val filter: TodoFilter,
    val isRefreshingTodos: Boolean,
)

private fun AppSettings.hasCloudConfig(): Boolean =
    platformEnabled && platformUrl.normalizePlatformUrlOrDefault().isNotBlank() && apiKey.isNotBlank()

private val QuickDraftZoneId: ZoneId = ZoneId.of("Asia/Shanghai")

private fun defaultQuickTodoDraft(defaultTodayEnabled: Boolean): QuickTodoDraft {
    val dueDate = if (defaultTodayEnabled) LocalDate.now(QuickDraftZoneId) else null
    return QuickTodoDraft(dueDate = dueDate)
}

private fun applyQuickDefaultDueDateIfSafe(
    draft: QuickTodoDraft,
    defaultTodayEnabled: Boolean,
): QuickTodoDraft {
    if (!draft.isUntouchedForDefaultDueDate()) {
        return draft
    }
    val today = LocalDate.now(QuickDraftZoneId)
    if (draft.dueDate != null && draft.dueDate != today) {
        return draft
    }
    val targetDueDate = if (defaultTodayEnabled) today else null
    return if (draft.dueDate == targetDueDate) draft else draft.copy(dueDate = targetDueDate)
}

private fun QuickTodoDraft.isUntouchedForDefaultDueDate(): Boolean =
    title.isBlank() &&
        description.isBlank() &&
        dueTimeText.isBlank() &&
        priority == 0 &&
        repeatType == RepeatType.NONE &&
        repeatWeekday == null &&
        repeatMonth == null &&
        repeatDay == null

private fun String.normalizePlatformUrl(): String = trim().trimEnd('/')

private fun String.normalizePlatformUrlOrDefault(): String =
    normalizePlatformUrl().ifBlank { DEFAULT_PLATFORM_URL }

private fun Throwable.toUserMessage(defaultMessage: String): String =
    message?.takeIf { it.isNotBlank() } ?: defaultMessage

private fun QuickTodoDraft.toTodoDraftInput(): TodoDraftInput {
    require(title.trim().isNotBlank()) { "请输入待办标题" }
    return TodoDraftInput(
        title = title.trim(),
        description = description.trim(),
        dueDate = dueDate,
        dueTime = dueTimeText.trim().takeIf { it.isNotEmpty() }?.parseTime("时间格式请使用 HH:mm"),
        repeatType = repeatType,
        repeatWeekday = if (repeatType == RepeatType.WEEKLY) repeatWeekday else null,
        repeatMonth = if (repeatType == RepeatType.YEARLY) repeatMonth else null,
        repeatDay = if (repeatType == RepeatType.MONTHLY || repeatType == RepeatType.YEARLY) repeatDay else null,
        priority = priority.coerceIn(0, 2),
        deviceId = null,
    )
}

private fun QuickTodoDraft.toEditorDraft(deviceName: String): TodoEditDraft = TodoEditDraft(
    title = title,
    description = description,
    dueDate = dueDate,
    dueTimeText = dueTimeText,
    priority = priority,
    repeatType = repeatType,
    repeatWeekday = repeatWeekday,
    repeatMonth = repeatMonth,
    repeatDay = repeatDay,
    deviceName = deviceName,
    createdAtLabel = "创建后生成",
    isCreateMode = true,
    isFromQuickComposer = true,
).normalized()

private fun Todo.toEditorDraft(): TodoEditDraft = TodoEditDraft(
    localId = localId,
    remoteId = remoteId,
    title = title,
    description = description,
    dueDate = dueDate,
    dueTimeText = dueTime?.toString().orEmpty(),
    priority = priority,
    repeatType = repeatType,
    repeatWeekday = repeatWeekday,
    repeatMonth = repeatMonth,
    repeatDay = repeatDay,
    deviceName = deviceName.orEmpty(),
    createdAtLabel = remoteCreateDate
        ?.takeIf { it.isNotBlank() }
        ?: Instant.ofEpochMilli(createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString()
            .replace('T', ' '),
    isCreateMode = false,
    isFromQuickComposer = false,
).normalized()

private fun TodoEditDraft.toTodoDraftInput(): TodoDraftInput {
    require(title.trim().isNotBlank()) { "请输入待办标题" }
    when (repeatType) {
        RepeatType.WEEKLY -> require(repeatWeekday != null) { "每周重复需要选择周几" }
        RepeatType.MONTHLY -> require(repeatDay in 1..31) { "每月重复需要选择 1-31 号" }
        RepeatType.YEARLY -> {
            require(repeatMonth in 1..12) { "每年重复需要选择 1-12 月" }
            require(repeatDay in 1..31) { "每年重复需要选择 1-31 号" }
        }

        else -> Unit
    }
    return TodoDraftInput(
        title = title.trim(),
        description = description.trim(),
        dueDate = dueDate,
        dueTime = dueTimeText.trim().takeIf { it.isNotEmpty() }?.parseTime("时间格式请使用 HH:mm"),
        repeatType = repeatType,
        repeatWeekday = if (repeatType == RepeatType.WEEKLY) repeatWeekday else null,
        repeatMonth = if (repeatType == RepeatType.YEARLY) repeatMonth else null,
        repeatDay = if (repeatType == RepeatType.MONTHLY || repeatType == RepeatType.YEARLY) repeatDay else null,
        priority = priority.coerceIn(0, 2),
        deviceId = null,
    )
}

private fun QuickTodoDraft.withRepeatType(type: RepeatType?): QuickTodoDraft {
    val sourceDate = dueDate ?: LocalDate.now()
    return when (type) {
        RepeatType.WEEKLY -> copy(
            repeatType = type,
            repeatWeekday = repeatWeekday ?: sourceDate.toWeekdayValue(),
            repeatMonth = null,
            repeatDay = null,
        )

        RepeatType.MONTHLY -> copy(
            repeatType = type,
            repeatWeekday = null,
            repeatMonth = null,
            repeatDay = repeatDay ?: sourceDate.dayOfMonth,
        )

        RepeatType.YEARLY -> copy(
            repeatType = type,
            repeatWeekday = null,
            repeatMonth = repeatMonth ?: sourceDate.monthValue,
            repeatDay = repeatDay ?: sourceDate.dayOfMonth,
        )

        RepeatType.DAILY, RepeatType.NONE -> copy(
            repeatType = type,
            repeatWeekday = null,
            repeatMonth = null,
            repeatDay = null,
        )

        null -> copy(
            repeatType = null,
            repeatWeekday = null,
            repeatMonth = null,
            repeatDay = null,
        )
    }
}

private fun TodoEditDraft.normalized(): TodoEditDraft {
    val sourceDate = dueDate ?: LocalDate.now()
    return when (repeatType) {
        RepeatType.WEEKLY -> copy(
            repeatWeekday = repeatWeekday ?: sourceDate.toWeekdayValue(),
            repeatMonth = null,
            repeatDay = null,
        )

        RepeatType.MONTHLY -> copy(
            repeatWeekday = null,
            repeatMonth = null,
            repeatDay = repeatDay ?: sourceDate.dayOfMonth,
        )

        RepeatType.YEARLY -> copy(
            repeatWeekday = null,
            repeatMonth = repeatMonth ?: sourceDate.monthValue,
            repeatDay = repeatDay ?: sourceDate.dayOfMonth,
        )

        RepeatType.DAILY, RepeatType.NONE -> copy(
            repeatWeekday = null,
            repeatMonth = null,
            repeatDay = null,
        )

        null -> copy(
            repeatWeekday = null,
            repeatMonth = null,
            repeatDay = null,
        )
    }
}

private fun LocalDate.toWeekdayValue(): Int = dayOfWeek.value % 7

private fun String.parseTime(errorMessage: String): LocalTime = try {
    LocalTime.parse(this)
} catch (_: DateTimeParseException) {
    error(errorMessage)
}


