package com.passheep.sticky_note.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.passheep.sticky_note.core.settings.AppAccentTheme
import com.passheep.sticky_note.core.settings.AppBackgroundStyle
import com.passheep.sticky_note.core.settings.AppSettings
import com.passheep.sticky_note.core.settings.DEFAULT_PLATFORM_URL
import com.passheep.sticky_note.core.settings.DefaultTodoFilter
import com.passheep.sticky_note.core.settings.PushType
import com.passheep.sticky_note.core.settings.SettingsRepository
import com.passheep.sticky_note.core.settings.ThemeMode
import com.passheep.sticky_note.core.settings.WidgetColorTheme
import com.passheep.sticky_note.core.settings.WidgetStylePreset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        val legacyMinutes = preferences[PreferenceKeys.LegacySyncIntervalMinutes]
        val syncIntervalSeconds = preferences[PreferenceKeys.SyncIntervalSeconds]
            ?: ((legacyMinutes ?: 5).coerceIn(1, 10) * 60)
        AppSettings(
            platformEnabled = preferences[PreferenceKeys.PlatformEnabled]
                ?: preferences[PreferenceKeys.ApiKey].isNullOrBlank().not(),
            platformUrl = preferences[PreferenceKeys.PlatformUrl]
                ?.takeIf { it.isNotBlank() }
                ?: DEFAULT_PLATFORM_URL,
            apiKey = preferences[PreferenceKeys.ApiKey].orEmpty(),
            syncIntervalSeconds = syncIntervalSeconds.coerceIn(60, 600),
            lastSyncAtMillis = preferences[PreferenceKeys.LastSyncAtMillis],
            selectedDeviceId = preferences[PreferenceKeys.SelectedDeviceId],
            selectedDeviceName = preferences[PreferenceKeys.SelectedDeviceName],
            themeMode = preferences[PreferenceKeys.ThemeMode].toThemeMode(),
            backgroundStyle = preferences[PreferenceKeys.BackgroundStyle].toBackgroundStyle(),
            accentTheme = preferences[PreferenceKeys.AccentTheme].toAccentTheme(),
            widgetThemeMode = preferences[PreferenceKeys.WidgetThemeMode].toThemeMode(),
            widgetStylePreset = preferences[PreferenceKeys.WidgetStylePreset].toWidgetStylePreset(),
            widgetColorTheme = preferences[PreferenceKeys.WidgetColorTheme].toWidgetColorTheme(),
            widgetTransparency = preferences[PreferenceKeys.WidgetTransparency] ?: 0.18f,
            widgetColorfulTextEnabled = preferences[PreferenceKeys.WidgetColorfulTextEnabled] ?: true,
            defaultPushType = preferences[PreferenceKeys.DefaultPushType].toPushType(),
            defaultPushPageId = preferences[PreferenceKeys.DefaultPushPageId] ?: 1,
            quickTodoDefaultTodayEnabled = preferences[PreferenceKeys.QuickTodoDefaultTodayEnabled] ?: false,
            defaultTodoFilter = preferences[PreferenceKeys.DefaultTodoFilter].toDefaultTodoFilter(),
            widgetDefaultFilter = preferences[PreferenceKeys.WidgetDefaultFilter].toDefaultTodoFilter(),
            widgetActiveFilter = (preferences[PreferenceKeys.WidgetActiveFilter]
                ?: preferences[PreferenceKeys.WidgetDefaultFilter]).toDefaultTodoFilter(),
        )
    }

    override suspend fun updatePlatformEnabled(value: Boolean) {
        dataStore.edit { it[PreferenceKeys.PlatformEnabled] = value }
    }

    override suspend fun updatePlatformUrl(value: String) {
        dataStore.edit { it[PreferenceKeys.PlatformUrl] = value.trim() }
    }

    override suspend fun updateApiKey(value: String) {
        dataStore.edit { it[PreferenceKeys.ApiKey] = value.trim() }
    }

    override suspend fun updateSyncIntervalSeconds(value: Int) {
        dataStore.edit { it[PreferenceKeys.SyncIntervalSeconds] = value.coerceIn(60, 600) }
    }

    override suspend fun updateLastSyncAtMillis(value: Long?) {
        dataStore.edit {
            if (value == null) {
                it.remove(PreferenceKeys.LastSyncAtMillis)
            } else {
                it[PreferenceKeys.LastSyncAtMillis] = value
            }
        }
    }

    override suspend fun updateSelectedDeviceId(value: String?) {
        dataStore.edit {
            if (value.isNullOrBlank()) {
                it.remove(PreferenceKeys.SelectedDeviceId)
            } else {
                it[PreferenceKeys.SelectedDeviceId] = value
            }
        }
    }

    override suspend fun updateSelectedDeviceName(value: String?) {
        dataStore.edit {
            if (value.isNullOrBlank()) {
                it.remove(PreferenceKeys.SelectedDeviceName)
            } else {
                it[PreferenceKeys.SelectedDeviceName] = value
            }
        }
    }

    override suspend fun updateThemeMode(value: ThemeMode) {
        dataStore.edit { it[PreferenceKeys.ThemeMode] = value.name }
    }

    override suspend fun updateBackgroundStyle(value: AppBackgroundStyle) {
        dataStore.edit { it[PreferenceKeys.BackgroundStyle] = value.name }
    }

    override suspend fun updateAccentTheme(value: AppAccentTheme) {
        dataStore.edit { it[PreferenceKeys.AccentTheme] = value.name }
    }

    override suspend fun updateWidgetThemeMode(value: ThemeMode) {
        dataStore.edit { it[PreferenceKeys.WidgetThemeMode] = value.name }
    }

    override suspend fun updateWidgetStylePreset(value: WidgetStylePreset) {
        dataStore.edit { it[PreferenceKeys.WidgetStylePreset] = value.name }
    }

    override suspend fun updateWidgetColorTheme(value: WidgetColorTheme) {
        dataStore.edit { it[PreferenceKeys.WidgetColorTheme] = value.name }
    }

    override suspend fun updateWidgetTransparency(value: Float) {
        dataStore.edit { it[PreferenceKeys.WidgetTransparency] = value.coerceIn(0f, 1f) }
    }

    override suspend fun updateWidgetColorfulTextEnabled(value: Boolean) {
        dataStore.edit { it[PreferenceKeys.WidgetColorfulTextEnabled] = value }
    }

    override suspend fun updateDefaultPushType(value: PushType) {
        dataStore.edit { it[PreferenceKeys.DefaultPushType] = value.name }
    }

    override suspend fun updateDefaultPushPageId(value: Int) {
        dataStore.edit { it[PreferenceKeys.DefaultPushPageId] = value.coerceIn(1, 5) }
    }

    override suspend fun updateQuickTodoDefaultTodayEnabled(value: Boolean) {
        dataStore.edit { it[PreferenceKeys.QuickTodoDefaultTodayEnabled] = value }
    }

    override suspend fun updateDefaultTodoFilter(value: DefaultTodoFilter) {
        dataStore.edit { it[PreferenceKeys.DefaultTodoFilter] = value.name }
    }

    override suspend fun updateWidgetDefaultFilter(value: DefaultTodoFilter) {
        dataStore.edit {
            it[PreferenceKeys.WidgetDefaultFilter] = value.name
            it[PreferenceKeys.WidgetActiveFilter] = value.name
        }
    }

    override suspend fun updateWidgetActiveFilter(value: DefaultTodoFilter) {
        dataStore.edit { it[PreferenceKeys.WidgetActiveFilter] = value.name }
    }
}

private fun String?.toDefaultTodoFilter(): DefaultTodoFilter =
    DefaultTodoFilter.entries.firstOrNull { it.name == this } ?: DefaultTodoFilter.ALL

private fun String?.toThemeMode(): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM

private fun String?.toBackgroundStyle(): AppBackgroundStyle =
    AppBackgroundStyle.entries.firstOrNull { it.name == this } ?: AppBackgroundStyle.PAPER

private fun String?.toAccentTheme(): AppAccentTheme =
    AppAccentTheme.entries.firstOrNull { it.name == this } ?: AppAccentTheme.INK_GREEN

private fun String?.toWidgetStylePreset(): WidgetStylePreset =
    WidgetStylePreset.entries.firstOrNull { it.name == this } ?: WidgetStylePreset.PAPER

private fun String?.toWidgetColorTheme(): WidgetColorTheme =
    WidgetColorTheme.entries.firstOrNull { it.name == this } ?: WidgetColorTheme.FOLLOW_APP

private fun String?.toPushType(): PushType =
    PushType.entries.firstOrNull { it.name == this } ?: PushType.STRUCTURED_TEXT
