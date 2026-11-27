package com.passheep.sticky_note.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.toArgb
import com.passheep.sticky_note.core.settings.AppAccentTheme
import com.passheep.sticky_note.core.settings.AppSettings
import com.passheep.sticky_note.core.settings.DefaultTodoFilter
import com.passheep.sticky_note.core.settings.ThemeMode
import com.passheep.sticky_note.core.settings.WidgetColorTheme
import com.passheep.sticky_note.core.settings.WidgetStylePreset
import com.passheep.sticky_note.ui.theme.PaperPalette
import com.passheep.sticky_note.ui.theme.toAccentSpec
import kotlin.math.roundToInt

/**
 * 桌面小组件解析后的完整渲染样式（同时供 RemoteViews 与设置页实时预览复用）
 * 注意：凡传入 ImageView.setColorFilter 的颜色（backgroundColor / borderColor / headerBandColor / addBadgeBgColor）
 * 均强制保持不透明 0xFFxxxxxx，避免 PorterDuff.Mode.SRC_ATOP 透出纯白底图造成 1~2px 白边。
 */
internal data class WidgetUiStyle(
    val preset: WidgetStylePreset,
    val colorTheme: WidgetColorTheme,
    val activeFilter: DefaultTodoFilter,
    val isDark: Boolean,
    val backgroundAlpha: Float,
    val borderAlpha: Float,
    val colorfulTextEnabled: Boolean,
    val useSharpCorner: Boolean,
    val showBorder: Boolean,
    val showHeaderBand: Boolean,
    val showAddBadge: Boolean,
    val showItemDivider: Boolean,
    val showDueDate: Boolean,
    val useSquareCheckbox: Boolean,
    val useFullSyncLabel: Boolean,
    val backgroundColor: Int,
    val borderColor: Int,
    val headerBandColor: Int,
    val headerPrimaryColor: Int,
    val headerSecondaryColor: Int,
    val tabSelectedBgColor: Int,
    val tabSelectedBgAlpha: Float,
    val tabSelectedTextColor: Int,
    val tabUnselectedTextColor: Int,
    val addBadgeBgColor: Int,
    val addButtonColor: Int,
    val accentColor: Int,
    val dividerColor: Int,
    val itemDividerColor: Int,
    val primaryTextColor: Int,
    val secondaryTextColor: Int,
    val pendingColor: Int,
    val urgentColor: Int,
    val importantColor: Int,
    val completedColor: Int,
)

/**
 * 小组件配色主题基础色板
 */
internal data class WidgetPaletteSpec(
    val label: String,
    val lightBg: Int,
    val darkBg: Int,
    val lightAccent: Int,
    val darkAccent: Int,
    val lightAccentSoft: Int,
    val darkAccentSoft: Int,
    val lightBand: Int,
    val darkBand: Int,
    val lightOnBand: Int,
    val darkOnBand: Int,
    val previewSwatch: Int,
)

internal object WidgetStyleResolver {

    fun paletteSpec(
        colorTheme: WidgetColorTheme,
        appAccentTheme: AppAccentTheme,
    ): WidgetPaletteSpec {
        val appAccent = appAccentTheme.toAccentSpec()
        return when (colorTheme) {
            WidgetColorTheme.FOLLOW_APP -> WidgetPaletteSpec(
                label = "跟随App",
                lightBg = PaperPalette.LightPaperCanvas.toArgb(),
                darkBg = PaperPalette.DarkPaperCanvas.toArgb(),
                lightAccent = appAccent.lightPrimary.toArgb(),
                darkAccent = appAccent.darkPrimary.toArgb(),
                lightAccentSoft = appAccent.lightSoft.toArgb(),
                darkAccentSoft = appAccent.darkSoft.toArgb(),
                lightBand = appAccent.lightPrimary.toArgb(),
                darkBand = appAccent.darkSoft.toArgb(),
                lightOnBand = appAccent.lightOnPrimary.toArgb(),
                darkOnBand = PaperPalette.DarkInkPrimary.toArgb(),
                previewSwatch = appAccent.previewSwatch.toArgb(),
            )

            WidgetColorTheme.INK_GREEN -> WidgetPaletteSpec(
                label = "墨青暖纸",
                lightBg = 0xFFF6F3EC.toInt(),
                darkBg = 0xFF191816.toInt(),
                lightAccent = 0xFF235D4E.toInt(),
                darkAccent = 0xFF6DB8A2.toInt(),
                lightAccentSoft = 0xFFE1EFEA.toInt(),
                darkAccentSoft = 0xFF1D352E.toInt(),
                lightBand = 0xFF235D4E.toInt(),
                darkBand = 0xFF213C34.toInt(),
                lightOnBand = 0xFFFFFFFF.toInt(),
                darkOnBand = 0xFFF2EFE8.toInt(),
                previewSwatch = 0xFF235D4E.toInt(),
            )

            WidgetColorTheme.BUTTER_NOTE -> WidgetPaletteSpec(
                label = "奶油便签",
                lightBg = 0xFFFDF3CE.toInt(),
                darkBg = 0xFF252016.toInt(),
                lightAccent = 0xFF965A10.toInt(),
                darkAccent = 0xFFE8B95E.toInt(),
                lightAccentSoft = 0xFFF7E4A6.toInt(),
                darkAccentSoft = 0xFF3A301C.toInt(),
                lightBand = 0xFFEAC256.toInt(),
                darkBand = 0xFF42351C.toInt(),
                lightOnBand = 0xFF2B1F0A.toInt(),
                darkOnBand = 0xFFFDF3CE.toInt(),
                previewSwatch = 0xFFE4B441.toInt(),
            )

            WidgetColorTheme.MIST_BLUE -> WidgetPaletteSpec(
                label = "雾蓝晴空",
                lightBg = 0xFFEEF4FB.toInt(),
                darkBg = 0xFF151C25.toInt(),
                lightAccent = 0xFF2B5282.toInt(),
                darkAccent = 0xFF84B0E3.toInt(),
                lightAccentSoft = 0xFFDAE6F6.toInt(),
                darkAccentSoft = 0xFF1D2E42.toInt(),
                lightBand = 0xFF2B5282.toInt(),
                darkBand = 0xFF21344B.toInt(),
                lightOnBand = 0xFFFFFFFF.toInt(),
                darkOnBand = 0xFFEEF4FB.toInt(),
                previewSwatch = 0xFF3B6CA8.toInt(),
            )

            WidgetColorTheme.BLOSSOM_PAPER -> WidgetPaletteSpec(
                label = "绯樱淡纸",
                lightBg = 0xFFFBECE8.toInt(),
                darkBg = 0xFF241B19.toInt(),
                lightAccent = 0xFF9B4935.toInt(),
                darkAccent = 0xFFE38E76.toInt(),
                lightAccentSoft = 0xFFF5D8D0.toInt(),
                darkAccentSoft = 0xFF3C2520.toInt(),
                lightBand = 0xFFBA6550.toInt(),
                darkBand = 0xFF412721.toInt(),
                lightOnBand = 0xFFFFFFFF.toInt(),
                darkOnBand = 0xFFFBECE8.toInt(),
                previewSwatch = 0xFFBA6550.toInt(),
            )

            WidgetColorTheme.MINT_LEAF -> WidgetPaletteSpec(
                label = "薄荷清露",
                lightBg = 0xFFEBF5F0.toInt(),
                darkBg = 0xFF15201C.toInt(),
                lightAccent = 0xFF246550.toInt(),
                darkAccent = 0xFF6EC4A7.toInt(),
                lightAccentSoft = 0xFFD4EBDF.toInt(),
                darkAccentSoft = 0xFF1D352C.toInt(),
                lightBand = 0xFF2E7860.toInt(),
                darkBand = 0xFF1F3B31.toInt(),
                lightOnBand = 0xFFFFFFFF.toInt(),
                darkOnBand = 0xFFEBF5F0.toInt(),
                previewSwatch = 0xFF2E7860.toInt(),
            )
        }
    }

    fun resolve(context: Context, settings: AppSettings): WidgetUiStyle {
        val isDark = when (settings.widgetThemeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> {
                val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMask == Configuration.UI_MODE_NIGHT_YES
            }
        }
        return resolveForPreview(
            preset = settings.widgetStylePreset,
            colorTheme = settings.widgetColorTheme,
            appAccentTheme = settings.accentTheme,
            isDark = isDark,
            transparency = settings.widgetTransparency,
            colorfulTextEnabled = settings.widgetColorfulTextEnabled,
            activeFilter = settings.widgetActiveFilter,
        )
    }

    /**
     * 无 Context 依赖的样式计算，供桌面渲染与设置页实时预览共用
     */
    fun resolveForPreview(
        preset: WidgetStylePreset,
        colorTheme: WidgetColorTheme,
        appAccentTheme: AppAccentTheme,
        isDark: Boolean,
        transparency: Float,
        colorfulTextEnabled: Boolean,
        activeFilter: DefaultTodoFilter = DefaultTodoFilter.ALL,
    ): WidgetUiStyle {
        val backgroundAlpha = (1f - transparency.coerceIn(0f, 0.92f)).coerceIn(0.08f, 1f)
        val chromeAlpha = (0.35f + backgroundAlpha * 0.65f).coerceIn(0.35f, 1f)
        val palette = paletteSpec(colorTheme, appAccentTheme)

        val urgentColor = if (isDark) PaperPalette.UrgentDark.toArgb() else PaperPalette.UrgentLight.toArgb()
        val importantColor = if (isDark) PaperPalette.ImportantDark.toArgb() else PaperPalette.ImportantLight.toArgb()
        val completedColor = if (isDark) PaperPalette.CompletedDark.toArgb() else PaperPalette.CompletedLight.toArgb()

        return when (preset) {
            WidgetStylePreset.CLASSIC -> {
                val useLegacyDefault = colorTheme == WidgetColorTheme.FOLLOW_APP
                val bg = opaque(
                    when {
                        useLegacyDefault && isDark -> 0xFF0B0F17.toInt()
                        useLegacyDefault -> 0xFFFFFFFF.toInt()
                        isDark -> palette.darkBg
                        else -> palette.lightBg
                    },
                )
                val accent = opaque(
                    when {
                        useLegacyDefault && isDark -> 0xFF7CC6FF.toInt()
                        useLegacyDefault -> 0xFF1D7CF2.toInt()
                        isDark -> palette.darkAccent
                        else -> palette.lightAccent
                    },
                )
                val primaryText = if (isDark) 0xFFF7F9FC.toInt() else 0xFF111827.toInt()
                val secondaryText = if (isDark) 0xFF9AA4B2.toInt() else 0xFF6B7280.toInt()
                val divider = withAlpha(if (isDark) 0xFF2A3344.toInt() else 0xFFE5E7EB.toInt(), chromeAlpha)
                val tabBg = opaque(if (isDark) 0xFF1D293B.toInt() else 0xFFE6EFFD.toInt())
                WidgetUiStyle(
                    preset = preset,
                    colorTheme = colorTheme,
                    activeFilter = activeFilter,
                    isDark = isDark,
                    backgroundAlpha = backgroundAlpha,
                    borderAlpha = 0f,
                    colorfulTextEnabled = colorfulTextEnabled,
                    useSharpCorner = false,
                    showBorder = false,
                    showHeaderBand = false,
                    showAddBadge = false,
                    showItemDivider = false,
                    showDueDate = false,
                    useSquareCheckbox = false,
                    useFullSyncLabel = false,
                    backgroundColor = bg,
                    borderColor = bg,
                    headerBandColor = bg,
                    headerPrimaryColor = primaryText,
                    headerSecondaryColor = secondaryText,
                    tabSelectedBgColor = tabBg,
                    tabSelectedBgAlpha = 0.92f,
                    tabSelectedTextColor = accent,
                    tabUnselectedTextColor = secondaryText,
                    addBadgeBgColor = accent,
                    addButtonColor = accent,
                    accentColor = accent,
                    dividerColor = divider,
                    itemDividerColor = divider,
                    primaryTextColor = primaryText,
                    secondaryTextColor = secondaryText,
                    pendingColor = if (isDark) 0xFFEAB308.toInt() else 0xFFB45309.toInt(),
                    urgentColor = urgentColor,
                    importantColor = importantColor,
                    completedColor = if (isDark) 0xFF34D399.toInt() else 0xFF047857.toInt(),
                )
            }

            WidgetStylePreset.PAPER -> {
                val bg = opaque(if (isDark) palette.darkBg else palette.lightBg)
                val accent = opaque(if (isDark) palette.darkAccent else palette.lightAccent)
                val accentSoft = opaque(if (isDark) palette.darkAccentSoft else palette.lightAccentSoft)
                val primaryText = if (isDark) PaperPalette.DarkInkPrimary.toArgb() else PaperPalette.LightInkPrimary.toArgb()
                val secondaryText = if (isDark) PaperPalette.DarkInkSecondary.toArgb() else PaperPalette.LightInkSecondary.toArgb()
                // 深色模式下移除外边框避免在深色壁纸上出现突兀亮边；浅色模式使用不透明柔和暖灰配低透明度
                val border = opaque(if (isDark) 0xFF32302B.toInt() else 0xFFD8D1C4.toInt())
                val divider = withAlpha(if (isDark) 0xFFF2EFE8.toInt() else 0xFF1C1B18.toInt(), 0.10f * chromeAlpha + 0.04f)
                WidgetUiStyle(
                    preset = preset,
                    colorTheme = colorTheme,
                    activeFilter = activeFilter,
                    isDark = isDark,
                    backgroundAlpha = backgroundAlpha,
                    borderAlpha = if (isDark) 0f else 0.55f,
                    colorfulTextEnabled = colorfulTextEnabled,
                    useSharpCorner = false,
                    showBorder = !isDark,
                    showHeaderBand = false,
                    showAddBadge = true,
                    showItemDivider = true,
                    showDueDate = true,
                    useSquareCheckbox = false,
                    useFullSyncLabel = false,
                    backgroundColor = bg,
                    borderColor = border,
                    headerBandColor = bg,
                    headerPrimaryColor = primaryText,
                    headerSecondaryColor = secondaryText,
                    tabSelectedBgColor = accentSoft,
                    tabSelectedBgAlpha = 0.96f,
                    tabSelectedTextColor = accent,
                    tabUnselectedTextColor = secondaryText,
                    addBadgeBgColor = accentSoft,
                    addButtonColor = accent,
                    accentColor = accent,
                    dividerColor = divider,
                    itemDividerColor = divider,
                    primaryTextColor = primaryText,
                    secondaryTextColor = secondaryText,
                    pendingColor = primaryText,
                    urgentColor = urgentColor,
                    importantColor = importantColor,
                    completedColor = completedColor,
                )
            }

            WidgetStylePreset.STICKY_NOTE -> {
                val bg = opaque(
                    when {
                        colorTheme == WidgetColorTheme.FOLLOW_APP && isDark -> 0xFF232018.toInt()
                        colorTheme == WidgetColorTheme.FOLLOW_APP -> 0xFFFDF5D6.toInt()
                        isDark -> palette.darkBg
                        else -> palette.lightBg
                    },
                )
                val accent = opaque(if (isDark) palette.darkAccent else palette.lightAccent)
                val bandColor = opaque(if (isDark) palette.darkBand else palette.lightBand)
                val onBand = if (isDark) palette.darkOnBand else palette.lightOnBand
                val primaryText = if (isDark) 0xFFF5F1E6.toInt() else 0xFF1E1D1A.toInt()
                val secondaryText = if (isDark) 0xFFBDB6A6.toInt() else 0xFF5D584B.toInt()
                val border = opaque(if (isDark) 0xFF3A3529.toInt() else 0xFFD9CCA6.toInt())
                val lineDivider = withAlpha(if (isDark) 0xFFF5F1E6.toInt() else 0xFF3E3520.toInt(), 0.11f)
                WidgetUiStyle(
                    preset = preset,
                    colorTheme = colorTheme,
                    activeFilter = activeFilter,
                    isDark = isDark,
                    backgroundAlpha = backgroundAlpha,
                    borderAlpha = if (isDark) 0f else 0.55f,
                    colorfulTextEnabled = colorfulTextEnabled,
                    useSharpCorner = false,
                    showBorder = !isDark,
                    showHeaderBand = true,
                    showAddBadge = false,
                    showItemDivider = true,
                    showDueDate = true,
                    useSquareCheckbox = false,
                    useFullSyncLabel = false,
                    backgroundColor = bg,
                    borderColor = border,
                    headerBandColor = bandColor,
                    headerPrimaryColor = onBand,
                    headerSecondaryColor = withAlpha(onBand, 0.85f),
                    tabSelectedBgColor = opaque(onBand),
                    tabSelectedBgAlpha = 0.22f,
                    tabSelectedTextColor = onBand,
                    tabUnselectedTextColor = withAlpha(onBand, 0.72f),
                    addBadgeBgColor = bandColor,
                    addButtonColor = onBand,
                    accentColor = accent,
                    dividerColor = 0x00000000,
                    itemDividerColor = lineDivider,
                    primaryTextColor = primaryText,
                    secondaryTextColor = secondaryText,
                    pendingColor = primaryText,
                    urgentColor = urgentColor,
                    importantColor = importantColor,
                    completedColor = completedColor,
                )
            }

            WidgetStylePreset.EINK -> {
                val bg = opaque(if (isDark) 0xFF141413.toInt() else 0xFFF4F3EE.toInt())
                val primaryText = if (isDark) 0xFFF2F1EC.toInt() else 0xFF141413.toInt()
                val secondaryText = if (isDark) 0xFFAFAEA7.toInt() else 0xFF52514C.toInt()
                val accent = opaque(
                    if (colorTheme == WidgetColorTheme.FOLLOW_APP) {
                        primaryText
                    } else {
                        if (isDark) palette.darkAccent else palette.lightAccent
                    },
                )
                // 墨水屏风格保留框线，但深色模式改为不透明深炭灰，彻底杜绝 SRC_ATOP 泛白
                val border = opaque(if (isDark) 0xFF474642.toInt() else 0xFF23221F.toInt())
                val headerDivider = withAlpha(primaryText, if (isDark) 0.36f else 0.72f)
                val rowDivider = withAlpha(primaryText, if (isDark) 0.16f else 0.16f)
                WidgetUiStyle(
                    preset = preset,
                    colorTheme = colorTheme,
                    activeFilter = activeFilter,
                    isDark = isDark,
                    backgroundAlpha = backgroundAlpha,
                    borderAlpha = if (isDark) 0.65f else 0.78f,
                    colorfulTextEnabled = colorfulTextEnabled,
                    useSharpCorner = true,
                    showBorder = true,
                    showHeaderBand = false,
                    showAddBadge = false,
                    showItemDivider = true,
                    showDueDate = true,
                    useSquareCheckbox = true,
                    useFullSyncLabel = false,
                    backgroundColor = bg,
                    borderColor = border,
                    headerBandColor = bg,
                    headerPrimaryColor = primaryText,
                    headerSecondaryColor = secondaryText,
                    tabSelectedBgColor = opaque(primaryText),
                    tabSelectedBgAlpha = 0.92f,
                    tabSelectedTextColor = bg,
                    tabUnselectedTextColor = secondaryText,
                    addBadgeBgColor = bg,
                    addButtonColor = accent,
                    accentColor = accent,
                    dividerColor = headerDivider,
                    itemDividerColor = rowDivider,
                    primaryTextColor = primaryText,
                    secondaryTextColor = secondaryText,
                    pendingColor = primaryText,
                    urgentColor = urgentColor,
                    importantColor = importantColor,
                    completedColor = completedColor,
                )
            }
        }
    }

    /**
     * 解析小组件单条待办的勾选符号颜色
     */
    fun radioColor(style: WidgetUiStyle, completed: Boolean, priority: Int): Int {
        if (style.preset == WidgetStylePreset.CLASSIC) {
            return todoTextColor(style, completed, priority)
        }
        if (!style.colorfulTextEnabled) {
            return if (completed) style.secondaryTextColor else style.primaryTextColor
        }
        return when {
            completed -> style.completedColor
            priority == 2 -> style.urgentColor
            priority == 1 -> style.importantColor
            else -> style.secondaryTextColor
        }
    }

    /**
     * 解析小组件单条待办的标题颜色
     */
    fun todoTextColor(style: WidgetUiStyle, completed: Boolean, priority: Int = 0): Int {
        if (style.preset == WidgetStylePreset.CLASSIC) {
            if (!style.colorfulTextEnabled) {
                if (!completed) return style.primaryTextColor
                val ratio = if (style.isDark) 0.58f else 0.42f
                return blendArgb(style.primaryTextColor, style.secondaryTextColor, ratio)
            }
            return if (completed) style.completedColor else style.pendingColor
        }
        if (completed) {
            val ratio = if (style.isDark) 0.65f else 0.58f
            return blendArgb(style.primaryTextColor, style.secondaryTextColor, ratio)
        }
        if (!style.colorfulTextEnabled || style.preset == WidgetStylePreset.EINK) {
            return style.primaryTextColor
        }
        return when (priority) {
            2 -> style.urgentColor
            1 -> style.importantColor
            else -> style.primaryTextColor
        }
    }

    private fun opaque(argb: Int): Int = argb or (0xFF shl 24)

    private fun withAlpha(argb: Int, alpha: Float): Int {
        val clamped = alpha.coerceIn(0f, 1f)
        val alphaInt = (clamped * 255f).roundToInt().coerceIn(0, 255)
        return (argb and 0x00FFFFFF) or (alphaInt shl 24)
    }

    private fun blendArgb(colorA: Int, colorB: Int, ratioB: Float): Int {
        val t = ratioB.coerceIn(0f, 1f)
        val aA = (colorA ushr 24) and 0xFF
        val rA = (colorA ushr 16) and 0xFF
        val gA = (colorA ushr 8) and 0xFF
        val bA = colorA and 0xFF

        val aB = (colorB ushr 24) and 0xFF
        val rB = (colorB ushr 16) and 0xFF
        val gB = (colorB ushr 8) and 0xFF
        val bB = colorB and 0xFF

        val a = (aA + ((aB - aA) * t)).roundToInt().coerceIn(0, 255)
        val r = (rA + ((rB - rA) * t)).roundToInt().coerceIn(0, 255)
        val g = (gA + ((gB - gA) * t)).roundToInt().coerceIn(0, 255)
        val b = (bA + ((bB - bA) * t)).roundToInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
