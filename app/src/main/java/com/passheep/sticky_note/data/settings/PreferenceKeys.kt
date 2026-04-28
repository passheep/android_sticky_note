package com.passheep.sticky_note.data.settings

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceKeys {
    val PlatformEnabled = booleanPreferencesKey("platform_enabled")
    val PlatformUrl = stringPreferencesKey("platform_url")
    val ApiKey = stringPreferencesKey("api_key")
    val SyncIntervalSeconds = intPreferencesKey("sync_interval_seconds")
    val LastSyncAtMillis = longPreferencesKey("last_sync_at_millis")
    val LegacySyncIntervalMinutes = intPreferencesKey("sync_interval_minutes")
    val SelectedDeviceId = stringPreferencesKey("selected_device_id")
    val SelectedDeviceName = stringPreferencesKey("selected_device_name")
    val ThemeMode = stringPreferencesKey("theme_mode")
    val WidgetThemeMode = stringPreferencesKey("widget_theme_mode")
    val WidgetTransparency = floatPreferencesKey("widget_transparency")
    val WidgetColorfulTextEnabled = booleanPreferencesKey("widget_colorful_text_enabled")
    val DefaultPushType = stringPreferencesKey("default_push_type")
    val DefaultPushPageId = intPreferencesKey("default_push_page_id")
    val QuickTodoDefaultTodayEnabled = booleanPreferencesKey("quick_todo_default_today_enabled")
}
