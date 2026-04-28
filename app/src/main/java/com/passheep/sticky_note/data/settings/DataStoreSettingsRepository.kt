package com.passheep.sticky_note.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.passheep.sticky_note.core.settings.AppSettings
import com.passheep.sticky_note.core.settings.DEFAULT_PLATFORM_URL
import com.passheep.sticky_note.core.settings.PushType
import com.passheep.sticky_note.core.settings.SettingsRepository
import com.passheep.sticky_note.core.settings.ThemeMode
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
            widgetThemeMode = preferences[PreferenceKeys.WidgetThemeMode].toThemeMode(),
            widgetTransparency = preferences[PreferenceKeys.WidgetTransparency] ?: 0.18f,
            widgetColorfulTextEnabled = preferences[PreferenceKeys.WidgetColorfulTextEnabled] ?: true,
            defaultPushType = preferences[PreferenceKeys.DefaultPushType].toPushType(),
            defaultPushPageId = preferences[PreferenceKeys.DefaultPushPageId] ?: 1,
            quickTodoDefaultTodayEnabled = preferences[PreferenceKeys.QuickTodoDefaultTodayEnabled] ?: false,
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

    override suspend fun updateWidgetThemeMode(value: ThemeMode) {
        dataStore.edit { it[PreferenceKeys.WidgetThemeMode] = value.name }
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
}

private fun String?.toThemeMode(): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM

private fun String?.toPushType(): PushType =
    PushType.entries.firstOrNull { it.name == this } ?: PushType.STRUCTURED_TEXT
