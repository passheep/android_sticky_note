package com.passheep.sticky_note.core.settings

const val DEFAULT_PLATFORM_URL = "https://cloud.zectrix.com"

data class AppSettings(
    val platformEnabled: Boolean = false,
    val platformUrl: String = DEFAULT_PLATFORM_URL,
    val apiKey: String = "",
    val syncIntervalSeconds: Int = 300,
    val lastSyncAtMillis: Long? = null,
    val selectedDeviceId: String? = null,
    val selectedDeviceName: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val widgetThemeMode: ThemeMode = ThemeMode.SYSTEM,
    val widgetTransparency: Float = 0.18f,
    val widgetColorfulTextEnabled: Boolean = true,
    val defaultPushType: PushType = PushType.STRUCTURED_TEXT,
    val defaultPushPageId: Int = 1,
    val quickTodoDefaultTodayEnabled: Boolean = false,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class PushType {
    TEXT,
    STRUCTURED_TEXT,
    IMAGE,
}
