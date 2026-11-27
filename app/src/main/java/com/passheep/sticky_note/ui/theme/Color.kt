package com.passheep.sticky_note.ui.theme

import androidx.compose.ui.graphics.Color
import com.passheep.sticky_note.core.settings.AppAccentTheme

/**
 * 暖纸墨韵基础调色板（日间与夜间共享色阶）
 */
object PaperPalette {
    // 日间纸面与墨色
    val LightPaperCanvas = Color(0xFFF6F3EC)
    val LightPaperSurface = Color(0xFFFFFDF9)
    val LightPaperSurfaceMuted = Color(0xFFEDE8DD)
    val LightPureCanvas = Color(0xFFFFFFFF)
    val LightPureSurface = Color(0xFFF6F5F1)
    val LightPureSurfaceMuted = Color(0xFFECEAE3)
    val LightInkPrimary = Color(0xFF1C1B18)
    val LightInkSecondary = Color(0xFF5A564D)
    val LightInkMuted = Color(0xFF6F6A5F)
    val LightHairline = Color(0x1A1C1B18)
    val LightBorderSubtle = Color(0x221C1B18)
    val LightGridStroke = Color(0x101C1B18)
    val LightDotStroke = Color(0x1E1C1B18)

    // 夜间暖炭纸面与柔光文字（低眩光，避免纯黑刺眼）
    val DarkPaperCanvas = Color(0xFF171614)
    val DarkPaperSurface = Color(0xFF21201D)
    val DarkPaperSurfaceMuted = Color(0xFF2B2925)
    val DarkPureCanvas = Color(0xFF111211)
    val DarkPureSurface = Color(0xFF1B1C1A)
    val DarkPureSurfaceMuted = Color(0xFF252623)
    val DarkInkPrimary = Color(0xFFF2EFE8)
    val DarkInkSecondary = Color(0xFFB8B2A6)
    val DarkInkMuted = Color(0xFF979185)
    val DarkHairline = Color(0x1EF2EFE8)
    val DarkBorderSubtle = Color(0x28F2EFE8)
    val DarkGridStroke = Color(0x12F2EFE8)
    val DarkDotStroke = Color(0x22F2EFE8)

    // 优先级与状态语义色（日间 / 夜间）
    val UrgentLight = Color(0xFFB83B28)
    val UrgentSoftLight = Color(0xFFF8E6E3)
    val UrgentDark = Color(0xFFEA7A68)
    val UrgentSoftDark = Color(0xFF3A201C)

    val ImportantLight = Color(0xFFA66314)
    val ImportantSoftLight = Color(0xFFF7ECD9)
    val ImportantDark = Color(0xFFE0A452)
    val ImportantSoftDark = Color(0xFF382A18)

    val CompletedLight = Color(0xFF2F6F5E)
    val CompletedDark = Color(0xFF69B39E)

    val BadgeDotLight = Color(0xFFC94430)
    val BadgeDotDark = Color(0xFFEA7A68)
}

/**
 * 主题强调色配置（日间/夜间双套高对比度调校）
 */
data class AccentThemeSpec(
    val lightPrimary: Color,
    val lightSoft: Color,
    val lightOnPrimary: Color,
    val darkPrimary: Color,
    val darkSoft: Color,
    val darkOnPrimary: Color,
    val previewSwatch: Color,
    val label: String,
)

/**
 * 获取各同风格主题强调色的语义色规格
 */
fun AppAccentTheme.toAccentSpec(): AccentThemeSpec = when (this) {
    AppAccentTheme.INK_GREEN -> AccentThemeSpec(
        lightPrimary = Color(0xFF235D4E),
        lightSoft = Color(0xFFE1EFEA),
        lightOnPrimary = Color(0xFFFFFFFF),
        darkPrimary = Color(0xFF6DB8A2),
        darkSoft = Color(0xFF1D352E),
        darkOnPrimary = Color(0xFF0E201A),
        previewSwatch = Color(0xFF235D4E),
        label = "墨青绿",
    )

    AppAccentTheme.INDIGO -> AccentThemeSpec(
        lightPrimary = Color(0xFF2B4F7A),
        lightSoft = Color(0xFFE1EBF6),
        lightOnPrimary = Color(0xFFFFFFFF),
        darkPrimary = Color(0xFF82ACE0),
        darkSoft = Color(0xFF1C2C40),
        darkOnPrimary = Color(0xFF0E1B2B),
        previewSwatch = Color(0xFF2B4F7A),
        label = "松烟靛",
    )

    AppAccentTheme.TERRACOTTA -> AccentThemeSpec(
        lightPrimary = Color(0xFF984B32),
        lightSoft = Color(0xFFF5E5DF),
        lightOnPrimary = Color(0xFFFFFFFF),
        darkPrimary = Color(0xFFE08C70),
        darkSoft = Color(0xFF3A231B),
        darkOnPrimary = Color(0xFF28120B),
        previewSwatch = Color(0xFF984B32),
        label = "赭陶棕",
    )

    AppAccentTheme.PLUM_INK -> AccentThemeSpec(
        lightPrimary = Color(0xFF5C456D),
        lightSoft = Color(0xFFEDE6F2),
        lightOnPrimary = Color(0xFFFFFFFF),
        darkPrimary = Color(0xFFB799CC),
        darkSoft = Color(0xFF2D2136),
        darkOnPrimary = Color(0xFF1E1326),
        previewSwatch = Color(0xFF5C456D),
        label = "黛藕紫",
    )

    AppAccentTheme.GRAPHITE -> AccentThemeSpec(
        lightPrimary = Color(0xFF2D2B27),
        lightSoft = Color(0xFFE7E3DA),
        lightOnPrimary = Color(0xFFFFFFFF),
        darkPrimary = Color(0xFFD1CBC0),
        darkSoft = Color(0xFF2F2D29),
        darkOnPrimary = Color(0xFF181715),
        previewSwatch = Color(0xFF2D2B27),
        label = "松墨灰",
    )
}
