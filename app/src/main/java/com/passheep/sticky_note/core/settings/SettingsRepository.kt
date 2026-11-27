package com.passheep.sticky_note.core.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun updatePlatformEnabled(value: Boolean)
    suspend fun updatePlatformUrl(value: String)
    suspend fun updateApiKey(value: String)
    suspend fun updateSyncIntervalSeconds(value: Int)
    suspend fun updateLastSyncAtMillis(value: Long?)
    suspend fun updateSelectedDeviceId(value: String?)
    suspend fun updateSelectedDeviceName(value: String?)
    suspend fun updateThemeMode(value: ThemeMode)
    suspend fun updateBackgroundStyle(value: AppBackgroundStyle)
    suspend fun updateAccentTheme(value: AppAccentTheme)
    suspend fun updateWidgetThemeMode(value: ThemeMode)
    suspend fun updateWidgetStylePreset(value: WidgetStylePreset)
    suspend fun updateWidgetColorTheme(value: WidgetColorTheme)
    suspend fun updateWidgetTransparency(value: Float)
    suspend fun updateWidgetColorfulTextEnabled(value: Boolean)
    suspend fun updateDefaultPushType(value: PushType)
    suspend fun updateDefaultPushPageId(value: Int)
    suspend fun updateQuickTodoDefaultTodayEnabled(value: Boolean)
    suspend fun updateDefaultTodoFilter(value: DefaultTodoFilter)
    suspend fun updateWidgetDefaultFilter(value: DefaultTodoFilter)
    suspend fun updateWidgetActiveFilter(value: DefaultTodoFilter)
}
