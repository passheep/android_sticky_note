package com.passheep.sticky_note

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.passheep.sticky_note.core.settings.AppAccentTheme
import com.passheep.sticky_note.core.settings.AppBackgroundStyle
import com.passheep.sticky_note.core.settings.ThemeMode
import com.passheep.sticky_note.ui.theme.PaperPalette
import com.passheep.sticky_note.ui.theme.toAccentSpec

/**
 * 暖纸墨韵语义色令牌，供所有页面与共享组件统一引用
 */
@Immutable
data class StickyNoteColors(
    val isDark: Boolean,
    val backgroundStyle: AppBackgroundStyle,
    val accentTheme: AppAccentTheme,
    val canvas: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val hairline: Color,
    val borderSubtle: Color,
    val patternStroke: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val urgent: Color,
    val urgentSoft: Color,
    val important: Color,
    val importantSoft: Color,
    val completed: Color,
    val badgeDot: Color,
)

private val DefaultStickyNoteColors = buildStickyNoteColors(
    useDark = false,
    backgroundStyle = AppBackgroundStyle.PAPER,
    accentTheme = AppAccentTheme.INK_GREEN,
)

val LocalStickyNoteColors = staticCompositionLocalOf { DefaultStickyNoteColors }

/**
 * 便捷访问当前主题下的暖纸语义色令牌
 */
object StickyNoteTokens {
    val colors: StickyNoteColors
        @Composable get() = LocalStickyNoteColors.current
}

/**
 * 暖纸墨韵字阶规范：层级清晰，正文行高舒展，数字通过局部 fontFeatureSettings = "tnum" 对齐
 */
private val StickyNoteTypography = Typography(
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    ),
)

/**
 * 应用顶层主题入口，联动日间/夜间模式、纸张背景材质与同风格主题强调色
 */
@Composable
fun StickyNoteTheme(
    themeMode: ThemeMode,
    backgroundStyle: AppBackgroundStyle = AppBackgroundStyle.PAPER,
    accentTheme: AppAccentTheme = AppAccentTheme.INK_GREEN,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val tokens = buildStickyNoteColors(
        useDark = useDark,
        backgroundStyle = backgroundStyle,
        accentTheme = accentTheme,
    )
    val colorScheme = tokens.toMaterialColorScheme()

    CompositionLocalProvider(LocalStickyNoteColors provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StickyNoteTypography,
            content = content,
        )
    }
}

private fun buildStickyNoteColors(
    useDark: Boolean,
    backgroundStyle: AppBackgroundStyle,
    accentTheme: AppAccentTheme,
): StickyNoteColors {
    val accentSpec = accentTheme.toAccentSpec()
    val isPure = backgroundStyle == AppBackgroundStyle.PURE

    return if (useDark) {
        StickyNoteColors(
            isDark = true,
            backgroundStyle = backgroundStyle,
            accentTheme = accentTheme,
            canvas = if (isPure) PaperPalette.DarkPureCanvas else PaperPalette.DarkPaperCanvas,
            surface = if (isPure) PaperPalette.DarkPureSurface else PaperPalette.DarkPaperSurface,
            surfaceMuted = if (isPure) PaperPalette.DarkPureSurfaceMuted else PaperPalette.DarkPaperSurfaceMuted,
            inkPrimary = PaperPalette.DarkInkPrimary,
            inkSecondary = PaperPalette.DarkInkSecondary,
            inkMuted = PaperPalette.DarkInkMuted,
            hairline = PaperPalette.DarkHairline,
            borderSubtle = PaperPalette.DarkBorderSubtle,
            patternStroke = when (backgroundStyle) {
                AppBackgroundStyle.DOT_GRID -> PaperPalette.DarkDotStroke
                else -> PaperPalette.DarkGridStroke
            },
            accent = accentSpec.darkPrimary,
            accentSoft = accentSpec.darkSoft,
            onAccent = accentSpec.darkOnPrimary,
            urgent = PaperPalette.UrgentDark,
            urgentSoft = PaperPalette.UrgentSoftDark,
            important = PaperPalette.ImportantDark,
            importantSoft = PaperPalette.ImportantSoftDark,
            completed = PaperPalette.CompletedDark,
            badgeDot = PaperPalette.BadgeDotDark,
        )
    } else {
        StickyNoteColors(
            isDark = false,
            backgroundStyle = backgroundStyle,
            accentTheme = accentTheme,
            canvas = if (isPure) PaperPalette.LightPureCanvas else PaperPalette.LightPaperCanvas,
            surface = if (isPure) PaperPalette.LightPureSurface else PaperPalette.LightPaperSurface,
            surfaceMuted = if (isPure) PaperPalette.LightPureSurfaceMuted else PaperPalette.LightPaperSurfaceMuted,
            inkPrimary = PaperPalette.LightInkPrimary,
            inkSecondary = PaperPalette.LightInkSecondary,
            inkMuted = PaperPalette.LightInkMuted,
            hairline = PaperPalette.LightHairline,
            borderSubtle = PaperPalette.LightBorderSubtle,
            patternStroke = when (backgroundStyle) {
                AppBackgroundStyle.DOT_GRID -> PaperPalette.LightDotStroke
                else -> PaperPalette.LightGridStroke
            },
            accent = accentSpec.lightPrimary,
            accentSoft = accentSpec.lightSoft,
            onAccent = accentSpec.lightOnPrimary,
            urgent = PaperPalette.UrgentLight,
            urgentSoft = PaperPalette.UrgentSoftLight,
            important = PaperPalette.ImportantLight,
            importantSoft = PaperPalette.ImportantSoftLight,
            completed = PaperPalette.CompletedLight,
            badgeDot = PaperPalette.BadgeDotLight,
        )
    }
}

private fun StickyNoteColors.toMaterialColorScheme(): ColorScheme = if (isDark) {
    darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = inkPrimary,
        secondary = completed,
        onSecondary = onAccent,
        secondaryContainer = surfaceMuted,
        onSecondaryContainer = inkPrimary,
        background = canvas,
        onBackground = inkPrimary,
        surface = surface,
        onSurface = inkPrimary,
        surfaceVariant = surfaceMuted,
        onSurfaceVariant = inkSecondary,
        surfaceContainerLowest = canvas,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceMuted,
        surfaceContainerHighest = surfaceMuted,
        outline = borderSubtle,
        outlineVariant = hairline,
        error = urgent,
        onError = onAccent,
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = inkPrimary,
        secondary = completed,
        onSecondary = onAccent,
        secondaryContainer = surfaceMuted,
        onSecondaryContainer = inkPrimary,
        background = canvas,
        onBackground = inkPrimary,
        surface = surface,
        onSurface = inkPrimary,
        surfaceVariant = surfaceMuted,
        onSurfaceVariant = inkSecondary,
        surfaceContainerLowest = surface,
        surfaceContainerLow = canvas,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surfaceMuted,
        outline = borderSubtle,
        outlineVariant = hairline,
        error = urgent,
        onError = onAccent,
    )
}
