package com.passheep.sticky_note.feature.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passheep.sticky_note.StickyNoteTokens
import com.passheep.sticky_note.core.model.RepeatType
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.settings.AppBackgroundStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal val BeijingZoneId: ZoneId = ZoneId.of("Asia/Shanghai")

internal val TimeOptions: List<String> = buildList {
    for (hour in 0..23) {
        add(String.format("%02d:00", hour))
        add(String.format("%02d:30", hour))
    }
}

private val LastSyncFullFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val LastSyncShortFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal fun Int.toPriorityLabel(): String = when (this) {
    2 -> "紧急"
    1 -> "重要"
    else -> "普通"
}

internal fun RepeatType?.toRepeatTypeLabel(): String = when (this) {
    null -> "不设置"
    RepeatType.NONE -> "不重复"
    RepeatType.DAILY -> "每天"
    RepeatType.WEEKLY -> "每周"
    RepeatType.MONTHLY -> "每月"
    RepeatType.YEARLY -> "每年"
}

internal fun Int.toWeekdayLabel(): String = when (this) {
    0 -> "周日"
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    else -> "周六"
}

internal fun LocalDate.toHeaderDateLabel(): String {
    val weekLabel = (dayOfWeek.value % 7).toWeekdayLabel()
    return "${monthValue}月${dayOfMonth}日 · $weekLabel"
}

internal fun LocalDate.toTodoDueDateLabel(): String {
    val today = LocalDate.now(BeijingZoneId)
    return when (this) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        today.minusDays(1) -> "昨天"
        else -> if (year == today.year) {
            String.format("%02d-%02d", monthValue, dayOfMonth)
        } else {
            toString()
        }
    }
}

internal fun Todo.dueDateTimeLabel(): String = buildString {
    if (dueDate != null) {
        append(dueDate.toTodoDueDateLabel())
        if (dueTime != null) {
            append(" ")
            append(dueTime)
        }
    } else if (dueTime != null) {
        append(dueTime)
    }
}

internal fun Todo.repeatSummaryLabel(): String? = when (repeatType) {
    RepeatType.DAILY -> "每天"
    RepeatType.WEEKLY -> "每周${(repeatWeekday ?: dueDate?.dayOfWeek?.value?.rem(7) ?: 0).toWeekdayLabel()}"
    RepeatType.MONTHLY -> repeatDay?.let { "每月${it}号" } ?: "每月"
    RepeatType.YEARLY -> when {
        repeatMonth != null && repeatDay != null -> "每年${repeatMonth}月${repeatDay}日"
        repeatMonth != null -> "每年${repeatMonth}月"
        repeatDay != null -> "每年${repeatDay}日"
        else -> "每年"
    }

    else -> null
}

internal fun LocalDate?.quickDateLabel(): String {
    val today = LocalDate.now(BeijingZoneId)
    return when (this) {
        null -> "不设日期"
        today -> "今天"
        today.plusDays(1) -> "明天"
        else -> String.format("%02d-%02d", monthValue, dayOfMonth)
    }
}

internal fun Long?.toLastSyncLabel(): String {
    if (this == null) return "暂未同步"
    return Instant.ofEpochMilli(this)
        .atZone(BeijingZoneId)
        .toLocalDateTime()
        .format(LastSyncFullFormatter)
}

internal fun Long?.toShortSyncTimeLabel(): String? {
    if (this == null) return null
    return Instant.ofEpochMilli(this)
        .atZone(BeijingZoneId)
        .toLocalDateTime()
        .format(LastSyncShortFormatter)
}

internal fun LocalDate.toDatePickerUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun Long.toDatePickerLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * 暖纸墨韵全屏画布，支持暖纸、纯白、方格网格与点阵手账四种实时材质绘制
 */
@Composable
fun PaperCanvasBackdrop(
    modifier: Modifier = Modifier,
    showSoftGlow: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = StickyNoteTokens.colors
    val animatedCanvas by animateColorAsState(
        targetValue = colors.canvas,
        animationSpec = tween(durationMillis = 220),
        label = "canvasColor",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(animatedCanvas)
            .drawWithCache {
                val widthPx = size.width.toInt().coerceAtLeast(1)
                val heightPx = size.height.toInt().coerceAtLeast(1)
                val glowRadius = size.width * 0.72f
                val glowCenter = Offset(size.width * 0.88f, size.height * 0.04f)
                val glowBrush = if (showSoftGlow && colors.backgroundStyle == AppBackgroundStyle.PAPER) {
                    Brush.radialGradient(
                        colors = listOf(
                            colors.accent.copy(alpha = if (colors.isDark) 0.11f else 0.06f),
                            Color.Transparent,
                        ),
                        center = glowCenter,
                        radius = glowRadius,
                    )
                } else {
                    null
                }

                // 将网格/点阵离屏光栅化为一张静态 ImageBitmap，120Hz 滚动时每帧仅 1 次 GPU 纹理贴图，杜绝掉帧
                val patternBitmap: ImageBitmap? = when (colors.backgroundStyle) {
                    AppBackgroundStyle.GRID -> {
                        val bmp = ImageBitmap(widthPx, heightPx)
                        val canvas = ComposeCanvas(bmp)
                        val paint = Paint().apply {
                            color = colors.patternStroke
                            strokeWidth = 0.65.dp.toPx()
                            style = PaintingStyle.Stroke
                            isAntiAlias = false
                        }
                        val stepPx = 22.dp.toPx()
                        var x = stepPx
                        while (x < widthPx) {
                            canvas.drawLine(Offset(x, 0f), Offset(x, heightPx.toFloat()), paint)
                            x += stepPx
                        }
                        var y = stepPx
                        while (y < heightPx) {
                            canvas.drawLine(Offset(0f, y), Offset(widthPx.toFloat(), y), paint)
                            y += stepPx
                        }
                        bmp
                    }

                    AppBackgroundStyle.DOT_GRID -> {
                        val bmp = ImageBitmap(widthPx, heightPx)
                        val canvas = ComposeCanvas(bmp)
                        val paint = Paint().apply {
                            color = colors.patternStroke
                            style = PaintingStyle.Fill
                            isAntiAlias = true
                        }
                        val stepPx = 18.dp.toPx()
                        val dotRadius = 0.95.dp.toPx()
                        var y = stepPx
                        while (y < heightPx) {
                            var x = stepPx
                            while (x < widthPx) {
                                canvas.drawCircle(Offset(x, y), dotRadius, paint)
                                x += stepPx
                            }
                            y += stepPx
                        }
                        bmp
                    }

                    AppBackgroundStyle.PAPER, AppBackgroundStyle.PURE -> null
                }

                onDrawBehind {
                    if (glowBrush != null) {
                        drawCircle(brush = glowBrush, radius = glowRadius, center = glowCenter)
                    }
                    if (patternBitmap != null) {
                        drawImage(patternBitmap)
                    }
                }
            },
        content = content,
    )
}

/**
 * 二级页面统一顶栏（返回圆形按钮 + 标题 + 可选右侧操作）
 */
@Composable
fun PaperSubPageTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = StickyNoteTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.borderSubtle),
            shadowElevation = if (colors.isDark) 0.dp else 1.dp,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = colors.inkPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.inkPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

/**
 * 页面分区小标题
 */
@Composable
fun PaperGroupLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = StickyNoteTokens.colors
    Text(
        text = text,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
        ),
        color = colors.inkMuted,
    )
}

/**
 * 暖纸软表面分组卡片容器（无厚重阴影，以轻描边与画布区分）
 */
@Composable
fun PaperSectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = StickyNoteTokens.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        contentColor = colors.inkPrimary,
        border = BorderStroke(0.8.dp, colors.borderSubtle),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content,
        )
    }
}

/**
 * 暖纸细分隔线
 */
@Composable
fun PaperHairline(
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp,
) {
    HorizontalDivider(
        modifier = modifier.padding(start = startIndent),
        thickness = 0.6.dp,
        color = StickyNoteTokens.colors.hairline,
    )
}

/**
 * 暖纸风单选分段切换控件（替代生硬的原生带勾 SegmentedButton）
 */
@Composable
fun <T> PaperSegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StickyNoteTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceMuted)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Surface(
                onClick = { onSelect(value) },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) colors.surface else Color.Transparent,
                shadowElevation = if (isSelected && !colors.isDark) 1.5.dp else 0.dp,
                tonalElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontFeatureSettings = "tnum",
                        ),
                        color = if (isSelected) colors.accent else colors.inkSecondary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * 暖纸柔和输入框（顶部标签 + 柔底圆角输入槽，聚焦切换细主题边框）
 */
@Composable
fun PaperTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    onFocusLost: (() -> Unit)? = null,
) {
    val colors = StickyNoteTokens.colors
    var isFocused by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkSecondary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 48.dp else 72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isFocused) colors.surface else colors.surfaceMuted.copy(alpha = 0.72f))
                .border(
                    width = if (isFocused) 1.2.dp else 0.8.dp,
                    color = if (isFocused) colors.accent else colors.borderSubtle,
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .onFocusChanged { state ->
                        if (hadFocus && !state.isFocused) {
                            onFocusLost?.invoke()
                        }
                        hadFocus = state.isFocused
                        isFocused = state.isFocused
                    },
                singleLine = singleLine,
                minLines = minLines,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = colors.inkPrimary,
                ),
                keyboardOptions = keyboardOptions,
                visualTransformation = visualTransformation,
                cursorBrush = SolidColor(colors.accent),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                color = colors.inkMuted,
                                maxLines = if (singleLine) 1 else 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

/**
 * 暖纸选择器字段（点击触发日期选择或弹层）
 */
@Composable
fun PaperPickerField(
    label: String,
    value: String?,
    placeholder: String,
    trailingIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StickyNoteTokens.colors
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.inkSecondary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            color = colors.surfaceMuted.copy(alpha = 0.72f),
            border = BorderStroke(0.8.dp, colors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value ?: placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = if (value == null) colors.inkMuted else colors.inkPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = colors.inkSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

internal data class PaperMenuOption<T>(
    val value: T,
    val label: String,
)

/**
 * 暖纸下拉选择字段
 */
@Composable
internal fun <T> PaperDropdownField(
    label: String,
    value: String?,
    placeholder: String,
    options: List<PaperMenuOption<T>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    menuMaxHeight: Dp = 260.dp,
) {
    val colors = StickyNoteTokens.colors
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        PaperPickerField(
            label = label,
            value = value,
            placeholder = placeholder,
            trailingIcon = Icons.Rounded.KeyboardArrowDown,
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            modifier = Modifier
                .heightIn(max = menuMaxHeight)
                .background(colors.surface),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    modifier = Modifier.height(44.dp),
                    text = {
                        Text(
                            text = option.label,
                            color = colors.inkPrimary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(option.value)
                    },
                )
            }
        }
    }
}

/**
 * 暖纸风开关设置行
 */
@Composable
fun PaperSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StickyNoteTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.inkPrimary,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkSecondary,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.surface,
                uncheckedTrackColor = colors.surfaceMuted,
                uncheckedBorderColor = colors.borderSubtle,
            ),
        )
    }
}

/**
 * 暖纸主操作按钮
 */
@Composable
fun PaperPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = StickyNoteTokens.colors
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            disabledContainerColor = colors.accent.copy(alpha = 0.45f),
            disabledContentColor = colors.onAccent.copy(alpha = 0.7f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * 暖纸次操作 / 警示按钮
 */
@Composable
fun PaperSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = StickyNoteTokens.colors
    val contentColor = if (destructive) colors.urgent else colors.inkPrimary
    val borderColor = if (destructive) colors.urgent.copy(alpha = 0.35f) else colors.borderSubtle
    val bgColor = if (destructive) colors.urgentSoft.copy(alpha = 0.55f) else colors.surfaceMuted.copy(alpha = 0.6f)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        contentColor = contentColor,
        border = BorderStroke(0.8.dp, borderColor),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}

/**
 * 暖纸导航/外链列表行
 */
@Composable
fun PaperNavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingText: String? = null,
    trailingIcon: ImageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
) {
    val colors = StickyNoteTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.inkPrimary,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkSecondary,
                )
            }
        }
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = colors.inkMuted,
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = colors.inkMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * 暖纸键值展示行
 */
@Composable
fun PaperInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = StickyNoteTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkSecondary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum",
            ),
            color = colors.inkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 暖纸空状态 / 引导说明卡片
 */
@Composable
fun PaperEmptyCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = StickyNoteTokens.colors
    PaperSectionCard(modifier = modifier, contentPadding = PaddingValues(20.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.inkPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkSecondary,
        )
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            Spacer(modifier = Modifier.height(14.dp))
            PaperPrimaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
