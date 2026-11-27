package com.passheep.sticky_note.feature.app

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passheep.sticky_note.BuildConfig
import com.passheep.sticky_note.StickyNoteTokens
import com.passheep.sticky_note.core.model.Device
import com.passheep.sticky_note.core.settings.AppAccentTheme
import com.passheep.sticky_note.core.settings.AppBackgroundStyle
import com.passheep.sticky_note.core.settings.DefaultTodoFilter
import com.passheep.sticky_note.core.settings.ThemeMode
import com.passheep.sticky_note.core.settings.WidgetColorTheme
import com.passheep.sticky_note.core.settings.WidgetStylePreset
import com.passheep.sticky_note.ui.theme.PaperPalette
import com.passheep.sticky_note.ui.theme.toAccentSpec
import com.passheep.sticky_note.widget.WidgetPinSize
import com.passheep.sticky_note.widget.WidgetStyleResolver
import com.passheep.sticky_note.widget.WidgetUiStyle
import com.passheep.sticky_note.widget.requestPinStickyNoteWidget

/**
 * 设置中心（含日间/夜间模式、纸张背景样式、同风格主题色、平台同步与小组件设置）
 */
@Composable
fun SettingsScreen(
    uiState: AppUiState,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onSetPlatformEnabled: (Boolean) -> Unit,
    onCommitPlatformUrl: (String) -> Unit,
    onCommitApiKey: (String) -> Unit,
    onSetSyncIntervalSeconds: (Int) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetBackgroundStyle: (AppBackgroundStyle) -> Unit,
    onSetAccentTheme: (AppAccentTheme) -> Unit,
    onSetWidgetThemeMode: (ThemeMode) -> Unit,
    onSetWidgetStylePreset: (WidgetStylePreset) -> Unit,
    onSetWidgetColorTheme: (WidgetColorTheme) -> Unit,
    onSetWidgetTransparency: (Float) -> Unit,
    onSetWidgetColorfulTextEnabled: (Boolean) -> Unit,
    onSetQuickTodoDefaultTodayEnabled: (Boolean) -> Unit,
    onSetDefaultTodoFilter: (DefaultTodoFilter) -> Unit,
    onSetWidgetDefaultFilter: (DefaultTodoFilter) -> Unit,
    onTestPlatformConnection: (String, String) -> Unit,
) {
    val colors = StickyNoteTokens.colors
    var platformUrlText by rememberSaveable(uiState.platformUrl) { mutableStateOf(uiState.platformUrl) }
    var apiKeyText by rememberSaveable(uiState.apiKey) { mutableStateOf(uiState.apiKey) }
    var showApiKey by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val alarmManager = remember(context) { context.getSystemService(AlarmManager::class.java) }
    val exactAlarmSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val exactAlarmGranted = !exactAlarmSupported || alarmManager?.canScheduleExactAlarms() == true

    Column(modifier = Modifier.fillMaxSize()) {
        PaperSubPageTopBar(
            title = "设置中心",
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // 1. 外观与纸张个性化设置
            item(key = "appearance") {
                Column {
                    PaperGroupLabel("外观与纸张")
                    PaperSectionCard(contentPadding = PaddingValues(16.dp)) {
                        Text(
                            text = "日间与夜间模式",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PaperSegmentedControl(
                            options = listOf(
                                ThemeMode.SYSTEM to "跟随系统",
                                ThemeMode.LIGHT to "日间",
                                ThemeMode.DARK to "夜间",
                            ),
                            selected = uiState.themeMode,
                            onSelect = onSetThemeMode,
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "纸张背景",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        BackgroundStylePickerRow(
                            selected = uiState.backgroundStyle,
                            isDarkPreview = colors.isDark,
                            onSelect = onSetBackgroundStyle,
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "主题强调色",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AccentThemePickerRow(
                            selected = uiState.accentTheme,
                            isDarkPreview = colors.isDark,
                            onSelect = onSetAccentTheme,
                        )
                    }
                }
            }

            // 1.5 首页与使用习惯
            item(key = "home_preferences") {
                Column {
                    PaperGroupLabel("首页与习惯")
                    PaperSectionCard(contentPadding = PaddingValues(16.dp)) {
                        Text(
                            text = "首页默认展示",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PaperSegmentedControl(
                            options = listOf(
                                DefaultTodoFilter.ALL to "全部",
                                DefaultTodoFilter.ACTIVE to "未完成",
                                DefaultTodoFilter.COMPLETED to "已完成",
                            ),
                            selected = uiState.defaultTodoFilter,
                            onSelect = onSetDefaultTodoFilter,
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        PaperHairline()
                        Spacer(modifier = Modifier.height(12.dp))
                        PaperSwitchRow(
                            title = "新增待办默认今天",
                            subtitle = "底部快速输入栏默认带上今日截止日期",
                            checked = uiState.quickTodoDefaultTodayEnabled,
                            onCheckedChange = onSetQuickTodoDefaultTodayEnabled,
                        )
                    }
                }
            }

            // 2. 平台与同步配置
            item(key = "platform") {
                Column {
                    PaperGroupLabel("平台与云端同步")
                    PaperSectionCard(contentPadding = PaddingValues(16.dp)) {
                        PaperSwitchRow(
                            title = "接入极趣云平台",
                            subtitle = "开启后待办清单与设备列表将直接同步云端数据",
                            checked = uiState.platformEnabled,
                            onCheckedChange = onSetPlatformEnabled,
                        )

                        if (uiState.platformEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            PaperHairline()
                            Spacer(modifier = Modifier.height(14.dp))

                            PaperTextField(
                                value = platformUrlText,
                                onValueChange = { platformUrlText = it },
                                label = "平台地址",
                                placeholder = "https://cloud.zectrix.com",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                onFocusLost = { onCommitPlatformUrl(platformUrlText) },
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            PaperTextField(
                                value = apiKeyText,
                                onValueChange = { apiKeyText = it },
                                label = "API 密钥",
                                placeholder = "在极趣云控制台获取",
                                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                onFocusLost = { onCommitApiKey(apiKeyText) },
                                trailing = {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .clickable { showApiKey = !showApiKey },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (showApiKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = if (showApiKey) "隐藏密钥" else "显示密钥",
                                            tint = colors.inkSecondary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                },
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "自动同步周期",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.inkPrimary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PaperSegmentedControl(
                                options = listOf(60 to "60s", 120 to "120s", 300 to "300s", 600 to "600s"),
                                selected = uiState.syncIntervalSeconds,
                                onSelect = onSetSyncIntervalSeconds,
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            PaperHairline()
                            Spacer(modifier = Modifier.height(6.dp))
                            PaperInfoRow("上次同步", uiState.lastSyncAtMillis.toLastSyncLabel())
                            if (exactAlarmSupported) {
                                PaperInfoRow("精确闹钟权限", if (exactAlarmGranted) "已开启" else "未开启")
                                if (!exactAlarmGranted) {
                                    Text(
                                        text = "未开启时系统可能将 60s 同步延后至约 70~120s。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.inkMuted,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    PaperSecondaryButton(
                                        text = "开启精确闹钟权限",
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            runCatching { context.startActivity(intent) }
                                                .onFailure {
                                                    Toast.makeText(
                                                        context,
                                                        "当前系统暂不支持直接跳转，请手动到系统设置中开启精确闹钟权限",
                                                        Toast.LENGTH_LONG,
                                                    ).show()
                                                }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            PaperPrimaryButton(
                                text = if (uiState.isTestingConnection) "正在测试连接..." else "测试平台连接",
                                enabled = !uiState.isTestingConnection,
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    onCommitPlatformUrl(platformUrlText)
                                    onCommitApiKey(apiKeyText)
                                    onTestPlatformConnection(platformUrlText, apiKeyText)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            // 3. 桌面小组件设置（内置实时预览 + 4 套风格 + 6 套主题色）
            item(key = "widget") {
                val widgetPreviewDark = when (uiState.widgetThemeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
                val previewStyle = remember(
                    uiState.widgetStylePreset,
                    uiState.widgetColorTheme,
                    uiState.accentTheme,
                    widgetPreviewDark,
                    uiState.widgetTransparency,
                    uiState.widgetColorfulTextEnabled,
                    uiState.widgetDefaultFilter,
                ) {
                    WidgetStyleResolver.resolveForPreview(
                        preset = uiState.widgetStylePreset,
                        colorTheme = uiState.widgetColorTheme,
                        appAccentTheme = uiState.accentTheme,
                        isDark = widgetPreviewDark,
                        transparency = uiState.widgetTransparency,
                        colorfulTextEnabled = uiState.widgetColorfulTextEnabled,
                        activeFilter = uiState.widgetDefaultFilter,
                    )
                }

                Column {
                    PaperGroupLabel("桌面小组件")
                    PaperSectionCard(contentPadding = PaddingValues(16.dp)) {
                        WidgetLivePreviewCard(style = previewStyle)

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "小组件默认展示",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PaperSegmentedControl(
                            options = listOf(
                                DefaultTodoFilter.ALL to "全部",
                                DefaultTodoFilter.ACTIVE to "未完成",
                                DefaultTodoFilter.COMPLETED to "已完成",
                            ),
                            selected = uiState.widgetDefaultFilter,
                            onSelect = onSetWidgetDefaultFilter,
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "小组件风格",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        WidgetStylePresetPicker(
                            selected = uiState.widgetStylePreset,
                            onSelect = onSetWidgetStylePreset,
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "小组件主题色",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        WidgetColorThemePicker(
                            selected = uiState.widgetColorTheme,
                            appAccentTheme = uiState.accentTheme,
                            isDarkPreview = widgetPreviewDark,
                            onSelect = onSetWidgetColorTheme,
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "小组件日间 / 夜间",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.inkPrimary,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PaperSegmentedControl(
                            options = listOf(
                                ThemeMode.SYSTEM to "跟随系统",
                                ThemeMode.LIGHT to "浅色",
                                ThemeMode.DARK to "深色",
                            ),
                            selected = uiState.widgetThemeMode,
                            onSelect = onSetWidgetThemeMode,
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        PaperSwitchRow(
                            title = "小组件彩色文字",
                            subtitle = "关闭后文字跟随小组件主题使用纯黑白色",
                            checked = uiState.widgetColorfulTextEnabled,
                            onCheckedChange = onSetWidgetColorfulTextEnabled,
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "小组件透明度",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.inkPrimary,
                            )
                            Text(
                                text = "${(uiState.widgetTransparency * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                color = colors.accent,
                            )
                        }
                        Slider(
                            value = uiState.widgetTransparency,
                            onValueChange = onSetWidgetTransparency,
                            valueRange = 0f..0.9f,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = colors.surfaceMuted,
                            ),
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "添加小组件至桌面（添加后可在桌面任意拖拽缩放）",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            WidgetPinSize.entries.forEach { pinSize ->
                                PaperSecondaryButton(
                                    text = "添加 ${pinSize.label}",
                                    onClick = {
                                        val pinned = requestPinStickyNoteWidget(context, pinSize)
                                        Toast.makeText(
                                            context,
                                            if (pinned) "已请求添加 ${pinSize.label} 小组件，请到桌面确认" else "当前桌面不支持一键添加，请长按桌面手动添加小组件",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            // 4. 其他（收拢“关于软件”入口，保持主页无底栏极简体验）
            item(key = "other") {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    PaperGroupLabel("其他")
                    PaperSectionCard(contentPadding = PaddingValues(0.dp)) {
                        PaperNavRow(
                            title = "关于软件",
                            subtitle = "版本信息、开源仓库与极趣云文档",
                            trailingText = "v${BuildConfig.VERSION_NAME}",
                            onClick = onOpenAbout,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4 种纸张背景的真实微缩可视化选择器（暖纸 / 纯白 / 方格 / 点阵）
 */
@Composable
private fun BackgroundStylePickerRow(
    selected: AppBackgroundStyle,
    isDarkPreview: Boolean,
    onSelect: (AppBackgroundStyle) -> Unit,
) {
    val colors = StickyNoteTokens.colors
    val items = listOf(
        AppBackgroundStyle.PAPER to "暖纸",
        AppBackgroundStyle.PURE to "纯白",
        AppBackgroundStyle.GRID to "方格",
        AppBackgroundStyle.DOT_GRID to "点阵",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { (style, label) ->
            val isSelected = style == selected
            val tileBg = when {
                isDarkPreview && style == AppBackgroundStyle.PURE -> PaperPalette.DarkPureCanvas
                isDarkPreview -> PaperPalette.DarkPaperCanvas
                style == AppBackgroundStyle.PURE -> PaperPalette.LightPureCanvas
                else -> PaperPalette.LightPaperCanvas
            }
            val patternColor = if (isDarkPreview) {
                if (style == AppBackgroundStyle.DOT_GRID) PaperPalette.DarkDotStroke else PaperPalette.DarkGridStroke
            } else {
                if (style == AppBackgroundStyle.DOT_GRID) PaperPalette.LightDotStroke else PaperPalette.LightGridStroke
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(style) },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tileBg)
                        .border(
                            width = if (isSelected) 2.dp else 0.9.dp,
                            color = if (isSelected) colors.accent else colors.borderSubtle,
                            shape = RoundedCornerShape(14.dp),
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithCache {
                                val w = size.width
                                val h = size.height
                                val gridPath = if (style == AppBackgroundStyle.GRID) {
                                    Path().apply {
                                        val step = 11.dp.toPx()
                                        var x = step
                                        while (x < w) {
                                            moveTo(x, 0f)
                                            lineTo(x, h)
                                            x += step
                                        }
                                        var y = step
                                        while (y < h) {
                                            moveTo(0f, y)
                                            lineTo(w, y)
                                            y += step
                                        }
                                    }
                                } else {
                                    null
                                }
                                val dots = if (style == AppBackgroundStyle.DOT_GRID) {
                                    buildList<Offset> {
                                        val step = 10.dp.toPx()
                                        var y = step
                                        while (y < h) {
                                            var x = step
                                            while (x < w) {
                                                add(Offset(x, y))
                                                x += step
                                            }
                                            y += step
                                        }
                                    }
                                } else {
                                    null
                                }
                                val stroke = Stroke(width = 0.8.dp.toPx())
                                val dotWidth = 2.2.dp.toPx()
                                val lineColor = if (isDarkPreview) Color(0x2EF2EFE8) else Color(0x261C1B18)
                                val lineStartX = 10.dp.toPx()
                                val lineStroke = 2.dp.toPx()

                                onDrawBehind {
                                    when (style) {
                                        AppBackgroundStyle.GRID -> if (gridPath != null) {
                                            drawPath(gridPath, patternColor, style = stroke)
                                        }

                                        AppBackgroundStyle.DOT_GRID -> if (!dots.isNullOrEmpty()) {
                                            drawPoints(
                                                points = dots,
                                                pointMode = PointMode.Points,
                                                color = patternColor,
                                                strokeWidth = dotWidth,
                                                cap = StrokeCap.Round,
                                            )
                                        }

                                        AppBackgroundStyle.PAPER, AppBackgroundStyle.PURE -> {
                                            drawLine(
                                                color = lineColor,
                                                start = Offset(lineStartX, h * 0.44f),
                                                end = Offset(w * 0.68f, h * 0.44f),
                                                strokeWidth = lineStroke,
                                            )
                                            drawLine(
                                                color = lineColor.copy(alpha = 0.6f),
                                                start = Offset(lineStartX, h * 0.64f),
                                                end = Offset(w * 0.48f, h * 0.64f),
                                                strokeWidth = lineStroke,
                                            )
                                        }
                                    }
                                }
                            },
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colors.onAccent,
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isDarkPreview && style == AppBackgroundStyle.PURE) "纯黑" else label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) colors.accent else colors.inkSecondary,
                )
            }
        }
    }
}

/**
 * 5 款同风格纸墨主题强调色色板选择器
 */
@Composable
private fun AccentThemePickerRow(
    selected: AppAccentTheme,
    isDarkPreview: Boolean,
    onSelect: (AppAccentTheme) -> Unit,
) {
    val colors = StickyNoteTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppAccentTheme.entries.forEach { accent ->
            val spec = accent.toAccentSpec()
            val isSelected = accent == selected
            val swatchColor = if (isDarkPreview) spec.darkPrimary else spec.lightPrimary
            val checkTint = if (isDarkPreview) spec.darkOnPrimary else spec.lightOnPrimary

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(accent) },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) swatchColor else Color.Transparent,
                            shape = CircleShape,
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(swatchColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = spec.label,
                            tint = checkTint,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = spec.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) colors.inkPrimary else colors.inkSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * 切换同步设备页
 */
@Composable
fun DeviceSwitchScreen(
    uiState: AppUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefreshDevices: () -> Unit,
    onSelectDevice: (String) -> Unit,
) {
    val colors = StickyNoteTokens.colors

    Column(modifier = Modifier.fillMaxSize()) {
        PaperSubPageTopBar(
            title = "切换设备",
            onBack = onBack,
            actions = {
                Surface(
                    onClick = onRefreshDevices,
                    enabled = !uiState.isRefreshingDevices,
                    shape = CircleShape,
                    color = colors.surface,
                    border = BorderStroke(0.8.dp, colors.borderSubtle),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (uiState.isRefreshingDevices) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = colors.accent,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "刷新设备",
                                tint = colors.inkPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            },
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!uiState.isCloudReady) {
                item(key = "cloud_not_ready") {
                    PaperEmptyCard(
                        title = "还没有完成平台配置",
                        body = "请先前往设置中心开启平台接入，并填写正确的平台地址和 API 密钥。",
                        actionLabel = "前往设置中心",
                        onAction = onOpenSettings,
                    )
                }
            } else if (uiState.devices.isEmpty()) {
                item(key = "no_devices") {
                    PaperEmptyCard(
                        title = "暂未获取到设备",
                        body = "点击右上角刷新按钮重试，或回到设置中心检查 API 密钥是否有效。",
                        actionLabel = if (uiState.isRefreshingDevices) "正在刷新..." else "重新拉取设备",
                        onAction = if (!uiState.isRefreshingDevices) onRefreshDevices else null,
                    )
                }
            } else {
                item(key = "devices_hint") {
                    PaperGroupLabel("选择需要同步的墨水屏设备（共 ${uiState.devices.size} 台）")
                }
                items(uiState.devices, key = Device::deviceId) { device ->
                    PaperDeviceCard(
                        device = device,
                        isSelected = device.deviceId == uiState.selectedDeviceId,
                        onClick = { onSelectDevice(device.deviceId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PaperDeviceCard(
    device: Device,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = StickyNoteTokens.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) colors.accentSoft.copy(alpha = 0.65f) else colors.surface,
        border = BorderStroke(
            width = if (isSelected) 1.4.dp else 0.8.dp,
            color = if (isSelected) colors.accent else colors.borderSubtle,
        ),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) colors.accent else colors.surfaceMuted),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Devices,
                    contentDescription = null,
                    tint = if (isSelected) colors.onAccent else colors.inkSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = device.alias.ifBlank { device.deviceId },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = buildString {
                        if (device.board.isNotBlank()) {
                            append(device.board)
                            append(" · ")
                        }
                        append(device.deviceId)
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = colors.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = colors.accent,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "当前设备",
                            tint = colors.onAccent,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "使用中",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onAccent,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 关于软件页
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    val colors = StickyNoteTokens.colors
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.fillMaxSize()) {
        PaperSubPageTopBar(
            title = "关于软件",
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "brand_card") {
                PaperSectionCard(contentPadding = PaddingValues(20.dp)) {
                    Text(
                        text = "智能便利贴",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.inkPrimary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "对接极趣云平台的轻量待办与电子墨水屏协同工具。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkSecondary,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PaperHairline()
                    Spacer(modifier = Modifier.height(4.dp))
                    PaperInfoRow("当前版本", "v${BuildConfig.VERSION_NAME}")
                    PaperInfoRow("开发者", "PASSHEEP")
                }
            }

            item(key = "links") {
                Column {
                    PaperGroupLabel("相关链接")
                    PaperSectionCard(contentPadding = PaddingValues(0.dp)) {
                        PaperNavRow(
                            title = "极趣云平台",
                            subtitle = "cloud.zectrix.com",
                            trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                            onClick = { uriHandler.openUri("https://cloud.zectrix.com") },
                        )
                        PaperHairline(startIndent = 16.dp)
                        PaperNavRow(
                            title = "项目 GitHub",
                            subtitle = "github.com/passheep/android_sticky_note",
                            trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                            onClick = { uriHandler.openUri("https://github.com/passheep/android_sticky_note") },
                        )
                        PaperHairline(startIndent = 16.dp)
                        PaperNavRow(
                            title = "极趣科技 Wiki",
                            subtitle = "wiki.zectrix.com",
                            trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                            onClick = { uriHandler.openUri("https://wiki.zectrix.com") },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 桌面小组件所见即所得实时预览卡（垫模拟柔和壁纸底板，直观反馈透明度、风格与主题色）
 */
@Composable
private fun WidgetLivePreviewCard(
    style: WidgetUiStyle,
) {
    val wallpaperBrush = if (style.isDark) {
        Brush.linearGradient(listOf(Color(0xFF28323F), Color(0xFF1F2730), Color(0xFF2A2633)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFC8D6E3), Color(0xFFD8D5CC), Color(0xFFC9D5CD)))
    }
    val cardCorner = if (style.useSharpCorner) 12.dp else 22.dp
    val sampleRows = remember(style.activeFilter) {
        val all = listOf(
            Triple("提交固件 OTA 说明", 2, "18:00"),
            Triple("京东白条还款", 1, "今天"),
            Triple("续费极趣云套餐", 0, "DONE"),
        )
        when (style.activeFilter) {
            DefaultTodoFilter.ALL -> all
            DefaultTodoFilter.ACTIVE -> all.filter { it.third != "DONE" }
            DefaultTodoFilter.COMPLETED -> all.filter { it.third == "DONE" }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(wallpaperBrush)
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cardCorner))
                .background(Color(style.backgroundColor).copy(alpha = style.backgroundAlpha))
                .then(
                    if (style.showBorder) {
                        Modifier.border(
                            width = if (style.useSharpCorner) 1.4.dp else 1.dp,
                            color = Color(style.borderColor).copy(alpha = style.borderAlpha),
                            shape = RoundedCornerShape(cardCorner),
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 顶栏（实体便利贴带彩色顶带，其他风格透明承托）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (style.showHeaderBand) {
                                Modifier.background(
                                    Color(style.headerBandColor).copy(
                                        alpha = (0.2f + style.backgroundAlpha * 0.8f).coerceIn(0.25f, 1f),
                                    ),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .then(
                                    if (style.showAddBadge) {
                                        Modifier
                                            .clip(CircleShape)
                                            .background(Color(style.addBadgeBgColor))
                                    } else {
                                        Modifier
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "+",
                                color = Color(style.addButtonColor),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            listOf(
                                DefaultTodoFilter.ALL to "全部",
                                DefaultTodoFilter.ACTIVE to "未完成",
                                DefaultTodoFilter.COMPLETED to "已完成",
                            ).forEach { (filter, tabLabel) ->
                                val isSelected = style.activeFilter == filter
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) {
                                                Color(style.tabSelectedBgColor).copy(alpha = style.tabSelectedBgAlpha)
                                            } else {
                                                Color.Transparent
                                            },
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = tabLabel,
                                        color = Color(if (isSelected) style.tabSelectedTextColor else style.tabUnselectedTextColor),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = if (style.useFullSyncLabel) "上次更新 13:09:28" else "13:09",
                            color = Color(style.headerSecondaryColor),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                fontFeatureSettings = "tnum",
                            ),
                        )
                        Text(
                            text = "\u21BB",
                            color = Color(style.headerSecondaryColor),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (!style.showHeaderBand) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = if (style.useSharpCorner) 1.dp else 0.7.dp,
                        color = Color(style.dividerColor),
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    sampleRows.forEachIndexed { index, (title, priority, dueTag) ->
                        val completed = dueTag == "DONE"
                        val radioColor = Color(WidgetStyleResolver.radioColor(style, completed, priority))
                        val titleColor = Color(WidgetStyleResolver.todoTextColor(style, completed, priority))
                        val symbol = when {
                            style.useSquareCheckbox && completed -> "\u2611"
                            style.useSquareCheckbox -> "\u2610"
                            completed -> "\u2713"
                            else -> "\u25CB"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = symbol,
                                color = radioColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(20.dp),
                            )
                            Text(
                                text = title,
                                color = titleColor,
                                fontSize = 13.sp,
                                fontWeight = if (style.preset == WidgetStylePreset.CLASSIC) FontWeight.SemiBold else FontWeight.Normal,
                                textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (style.showDueDate && !completed) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = dueTag,
                                    color = Color(
                                        if (priority == 2 && style.colorfulTextEnabled) style.urgentColor else style.accentColor,
                                    ),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        fontFeatureSettings = "tnum",
                                    ),
                                )
                            }
                        }

                        if (style.showItemDivider && index < sampleRows.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 20.dp),
                                thickness = 0.6.dp,
                                color = Color(style.itemDividerColor),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 桌面小组件 4 套风格选择器（2×2 卡片）
 */
@Composable
private fun WidgetStylePresetPicker(
    selected: WidgetStylePreset,
    onSelect: (WidgetStylePreset) -> Unit,
) {
    val rows = listOf(
        listOf(
            WidgetStylePreset.PAPER to "暖纸墨韵" to "与 App 风格一致",
            WidgetStylePreset.CLASSIC to "经典原版" to "保留原有简约样式",
        ),
        listOf(
            WidgetStylePreset.STICKY_NOTE to "实体便利贴" to "顶部色带手账感",
            WidgetStylePreset.EINK to "极简墨水屏" to "高对比方框线风",
        ),
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { (presetAndTitle, desc) ->
                    val (preset, title) = presetAndTitle
                    val isSelected = preset == selected
                    val colors = StickyNoteTokens.colors

                    Surface(
                        onClick = { onSelect(preset) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) colors.accentSoft.copy(alpha = 0.72f) else colors.surfaceMuted.copy(alpha = 0.65f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.8.dp,
                            color = if (isSelected) colors.accent else colors.borderSubtle,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) colors.accent else colors.inkPrimary,
                                    maxLines = 1,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = colors.inkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = colors.onAccent,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 桌面小组件 6 套配色主题选择器（2×3 色板）
 */
@Composable
private fun WidgetColorThemePicker(
    selected: WidgetColorTheme,
    appAccentTheme: AppAccentTheme,
    isDarkPreview: Boolean,
    onSelect: (WidgetColorTheme) -> Unit,
) {
    val colors = StickyNoteTokens.colors
    val rows = WidgetColorTheme.entries.chunked(3)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { rowThemes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowThemes.forEach { theme ->
                    val spec = WidgetStyleResolver.paletteSpec(theme, appAccentTheme)
                    val isSelected = theme == selected
                    val bgPreview = Color(if (isDarkPreview) spec.darkBg else spec.lightBg)
                    val accentPreview = Color(if (isDarkPreview) spec.darkAccent else spec.lightAccent)

                    Surface(
                        onClick = { onSelect(theme) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) colors.accentSoft.copy(alpha = 0.65f) else colors.surfaceMuted.copy(alpha = 0.5f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.8.dp,
                            color = if (isSelected) colors.accent else colors.borderSubtle,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 9.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(bgPreview)
                                    .border(1.dp, colors.borderSubtle, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accentPreview),
                                )
                            }
                            Text(
                                text = spec.label,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) colors.inkPrimary else colors.inkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
