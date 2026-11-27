package com.passheep.sticky_note.core.settings

const val DEFAULT_PLATFORM_URL = "https://cloud.zectrix.com"

/**
 * 应用全局配置状态
 */
data class AppSettings(
    val platformEnabled: Boolean = false,
    val platformUrl: String = DEFAULT_PLATFORM_URL,
    val apiKey: String = "",
    val syncIntervalSeconds: Int = 300,
    val lastSyncAtMillis: Long? = null,
    val selectedDeviceId: String? = null,
    val selectedDeviceName: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val backgroundStyle: AppBackgroundStyle = AppBackgroundStyle.PAPER,
    val accentTheme: AppAccentTheme = AppAccentTheme.INK_GREEN,
    val widgetThemeMode: ThemeMode = ThemeMode.SYSTEM,
    val widgetStylePreset: WidgetStylePreset = WidgetStylePreset.PAPER,
    val widgetColorTheme: WidgetColorTheme = WidgetColorTheme.FOLLOW_APP,
    val widgetTransparency: Float = 0.18f,
    val widgetColorfulTextEnabled: Boolean = true,
    val defaultPushType: PushType = PushType.STRUCTURED_TEXT,
    val defaultPushPageId: Int = 1,
    val quickTodoDefaultTodayEnabled: Boolean = false,
    val defaultTodoFilter: DefaultTodoFilter = DefaultTodoFilter.ALL,
    val widgetDefaultFilter: DefaultTodoFilter = DefaultTodoFilter.ALL,
    val widgetActiveFilter: DefaultTodoFilter = DefaultTodoFilter.ALL,
)

/**
 * 首页默认展示的待办筛选分类
 */
enum class DefaultTodoFilter {
    ALL,
    ACTIVE,
    COMPLETED,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * 应用画布背景材质样式
 */
enum class AppBackgroundStyle {
    PAPER,
    PURE,
    GRID,
    DOT_GRID,
}

/**
 * 应用暖纸墨韵同风格主题强调色
 */
enum class AppAccentTheme {
    INK_GREEN,
    INDIGO,
    TERRACOTTA,
    PLUM_INK,
    GRAPHITE,
}

/**
 * 桌面小组件风格（保留经典原版，并新增暖纸、便利贴与墨水屏风格）
 */
enum class WidgetStylePreset {
    CLASSIC,
    PAPER,
    STICKY_NOTE,
    EINK,
}

/**
 * 桌面小组件配色主题
 */
enum class WidgetColorTheme {
    FOLLOW_APP,
    INK_GREEN,
    BUTTER_NOTE,
    MIST_BLUE,
    BLOSSOM_PAPER,
    MINT_LEAF,
}

enum class PushType {
    TEXT,
    STRUCTURED_TEXT,
    IMAGE,
}
