package com.passheep.sticky_note.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.passheep.sticky_note.MainActivity
import com.passheep.sticky_note.R
import com.passheep.sticky_note.core.settings.DefaultTodoFilter
import dagger.hilt.android.EntryPointAccessors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

object StickyNoteRemoteWidgetRenderer {

    suspend fun refreshAllFromCloud(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = StickyNoteWidgetReceiver.allWidgetIds(context)
        if (widgetIds.isEmpty()) {
            Log.d(TAG, "refreshAllFromCloud skipped no_widget")
            return
        }

        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val settings = entryPoint.settingsRepository().settings.first()
        val style = WidgetStyleResolver.resolve(context, settings)
        val hasCloudConfig = settings.platformEnabled && settings.apiKey.isNotBlank()
        val lastSyncLabel = settings.lastSyncAtMillis.toWidgetLastSyncLabel()

        Log.d(
            TAG,
            "refreshAllFromCloud widgets=${widgetIds.size} preset=${style.preset} filter=${style.activeFilter} dark=${style.isDark}",
        )

        appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_todo_list)

        widgetIds.forEach { widgetId ->
            val minWidthDp = appWidgetManager.getAppWidgetOptions(widgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            val showSyncStamp = minWidthDp >= 220
            val views = buildWidgetViews(
                context = context,
                widgetId = widgetId,
                hasCloudConfig = hasCloudConfig,
                lastSyncLabel = lastSyncLabel,
                showSyncStamp = showSyncStamp,
                style = style,
            )
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun buildWidgetViews(
        context: Context,
        widgetId: Int,
        hasCloudConfig: Boolean,
        lastSyncLabel: String,
        showSyncStamp: Boolean,
        style: WidgetUiStyle,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.sticky_note_widget_layout)
        views.setTextViewText(R.id.widget_add_button, "+")
        views.setTextViewText(R.id.widget_refresh_button, "\u21BB")
        views.setViewVisibility(R.id.widget_last_sync_text, if (showSyncStamp) View.VISIBLE else View.GONE)
        if (showSyncStamp) {
            views.setTextViewText(R.id.widget_last_sync_text, lastSyncLabel)
        }

        val emptyText = when {
            !hasCloudConfig -> "请先配置平台"
            style.activeFilter == DefaultTodoFilter.ACTIVE -> "暂无未完成待办"
            style.activeFilter == DefaultTodoFilter.COMPLETED -> "暂无已完成待办"
            else -> "暂无待办"
        }
        views.setTextViewText(R.id.widget_empty_text, emptyText)

        // 背景形状、着色与透明度（不透明 colorFilter + 独立 alpha，杜绝白边）
        val fillDrawable = if (style.useSharpCorner) R.drawable.widget_shape_fill_14 else R.drawable.widget_shape_fill_26
        val strokeDrawable = if (style.useSharpCorner) R.drawable.widget_shape_stroke_14 else R.drawable.widget_shape_stroke_26
        views.setImageViewResource(R.id.widget_bg_layer, fillDrawable)
        views.setInt(R.id.widget_bg_layer, "setColorFilter", style.backgroundColor)
        views.setFloat(R.id.widget_bg_layer, "setAlpha", style.backgroundAlpha)

        // 描边层
        views.setViewVisibility(R.id.widget_stroke_layer, if (style.showBorder) View.VISIBLE else View.GONE)
        if (style.showBorder) {
            views.setImageViewResource(R.id.widget_stroke_layer, strokeDrawable)
            views.setInt(R.id.widget_stroke_layer, "setColorFilter", style.borderColor)
            views.setFloat(
                R.id.widget_stroke_layer,
                "setAlpha",
                (style.borderAlpha * (0.35f + style.backgroundAlpha * 0.65f)).coerceIn(0f, 1f),
            )
        }

        // 实体便利贴顶部色带
        views.setViewVisibility(R.id.widget_header_band, if (style.showHeaderBand) View.VISIBLE else View.GONE)
        if (style.showHeaderBand) {
            views.setInt(R.id.widget_header_band, "setColorFilter", style.headerBandColor)
            views.setFloat(R.id.widget_header_band, "setAlpha", (0.2f + style.backgroundAlpha * 0.8f).coerceIn(0.25f, 1f))
        }

        // 暖纸风格的 + 圆形浅底徽章
        views.setViewVisibility(R.id.widget_add_badge_bg, if (style.showAddBadge) View.VISIBLE else View.GONE)
        if (style.showAddBadge) {
            views.setInt(R.id.widget_add_badge_bg, "setColorFilter", style.addBadgeBgColor)
        }

        // 顶部 3 个筛选 Tab（全部 / 未完成 / 已完成）
        bindFilterTab(
            views = views,
            context = context,
            widgetId = widgetId,
            filter = DefaultTodoFilter.ALL,
            label = "全部",
            containerId = R.id.widget_tab_all,
            bgId = R.id.widget_tab_all_bg,
            textId = R.id.widget_tab_all_text,
            style = style,
        )
        bindFilterTab(
            views = views,
            context = context,
            widgetId = widgetId,
            filter = DefaultTodoFilter.ACTIVE,
            label = "未完成",
            containerId = R.id.widget_tab_active,
            bgId = R.id.widget_tab_active_bg,
            textId = R.id.widget_tab_active_text,
            style = style,
        )
        bindFilterTab(
            views = views,
            context = context,
            widgetId = widgetId,
            filter = DefaultTodoFilter.COMPLETED,
            label = "已完成",
            containerId = R.id.widget_tab_completed,
            bgId = R.id.widget_tab_completed_bg,
            textId = R.id.widget_tab_completed_text,
            style = style,
        )

        // 顶部分隔线与文字颜色
        views.setViewVisibility(R.id.widget_divider, if (style.showHeaderBand) View.INVISIBLE else View.VISIBLE)
        views.setInt(R.id.widget_divider, "setBackgroundColor", style.dividerColor)
        views.setTextColor(R.id.widget_add_button, style.addButtonColor)
        views.setTextColor(R.id.widget_refresh_button, style.headerSecondaryColor)
        views.setTextColor(R.id.widget_last_sync_text, style.headerSecondaryColor)
        views.setTextColor(R.id.widget_empty_text, style.primaryTextColor)

        views.setOnClickPendingIntent(
            R.id.widget_add_container,
            quickCreatePendingIntent(context = context, widgetId = widgetId),
        )
        views.setOnClickPendingIntent(
            R.id.widget_refresh_button,
            refreshPendingIntent(context = context, widgetId = widgetId),
        )
        views.setOnClickPendingIntent(
            R.id.widget_last_sync_text,
            openHomePendingIntent(context = context, widgetId = widgetId),
        )

        val listIntent = Intent(context, StickyNoteWidgetListService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse("stickynote://widget/list/$widgetId")
        }
        views.setRemoteAdapter(R.id.widget_todo_list, listIntent)
        views.setEmptyView(R.id.widget_todo_list, R.id.widget_empty_text)
        views.setPendingIntentTemplate(
            R.id.widget_todo_list,
            itemTemplatePendingIntent(context = context, widgetId = widgetId),
        )
        return views
    }

    private fun bindFilterTab(
        views: RemoteViews,
        context: Context,
        widgetId: Int,
        filter: DefaultTodoFilter,
        label: String,
        containerId: Int,
        bgId: Int,
        textId: Int,
        style: WidgetUiStyle,
    ) {
        val selected = style.activeFilter == filter
        views.setViewVisibility(bgId, if (selected) View.VISIBLE else View.GONE)
        if (selected) {
            views.setInt(bgId, "setColorFilter", style.tabSelectedBgColor)
            views.setFloat(bgId, "setAlpha", style.tabSelectedBgAlpha)
        }
        val styledText: CharSequence = if (selected) {
            SpannableString(label).apply {
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else {
            label
        }
        views.setTextViewText(textId, styledText)
        views.setTextColor(textId, if (selected) style.tabSelectedTextColor else style.tabUnselectedTextColor)
        views.setOnClickPendingIntent(
            containerId,
            filterPendingIntent(context = context, widgetId = widgetId, filter = filter),
        )
    }

    private fun filterPendingIntent(
        context: Context,
        widgetId: Int,
        filter: DefaultTodoFilter,
    ): PendingIntent {
        val intent = Intent(context, StickyNoteWidgetReceiver::class.java).apply {
            action = StickyNoteWidgetReceiver.ACTION_WIDGET_FILTER
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(StickyNoteWidgetReceiver.EXTRA_FILTER, filter.name)
            data = Uri.parse("stickynote://widget/filter/$widgetId/${filter.name}")
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode("filter_${filter.name}", widgetId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun refreshPendingIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, StickyNoteWidgetReceiver::class.java).apply {
            action = StickyNoteWidgetReceiver.ACTION_WIDGET_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse("stickynote://widget/refresh/$widgetId")
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode("refresh", widgetId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun itemTemplatePendingIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, StickyNoteWidgetReceiver::class.java).apply {
            action = StickyNoteWidgetReceiver.ACTION_WIDGET_ITEM
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse("stickynote://widget/item_template/$widgetId")
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode("item_template", widgetId),
            intent,
            pendingIntentTemplateFlags(),
        )
    }

    private fun openHomePendingIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data = Uri.parse("stickynote://widget/open/home/$widgetId")
        }
        return PendingIntent.getActivity(
            context,
            requestCode("open_home", widgetId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun quickCreatePendingIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_OPEN_QUICK_EDITOR, true)
            data = Uri.parse("stickynote://widget/open/quick/$widgetId")
        }
        return PendingIntent.getActivity(
            context,
            requestCode("open_quick", widgetId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCode(key: String, widgetId: Int, extra: Any? = null): Int {
        var result = 17
        result = 31 * result + key.hashCode()
        result = 31 * result + widgetId
        result = 31 * result + (extra?.hashCode() ?: 0)
        return result
    }

    private fun pendingIntentTemplateFlags(): Int {
        val baseFlags = PendingIntent.FLAG_UPDATE_CURRENT
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            baseFlags or PendingIntent.FLAG_MUTABLE
        } else {
            baseFlags
        }
    }

    private fun Long?.toWidgetLastSyncLabel(): String {
        if (this == null) return "--:--"
        return Instant.ofEpochMilli(this)
            .atZone(BEIJING_ZONE_ID)
            .toLocalDateTime()
            .format(LAST_SYNC_SHORT_FORMATTER)
    }

    private val BEIJING_ZONE_ID: ZoneId = ZoneId.of("Asia/Shanghai")
    private val LAST_SYNC_SHORT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private const val TAG = "StickyNoteWidgetRndr"
}
