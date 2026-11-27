package com.passheep.sticky_note.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.passheep.sticky_note.R
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.settings.AppAccentTheme
import com.passheep.sticky_note.core.settings.DefaultTodoFilter
import com.passheep.sticky_note.core.settings.WidgetColorTheme
import com.passheep.sticky_note.core.settings.WidgetStylePreset
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * 桌面小组件完成/取消完成两阶段动效状态表：
 * 记录正在播放过渡脉冲的 remoteId -> 目标完成态，使点击后 0ms 展现高亮过渡并在收尾后自然落定。
 */
internal object WidgetToggleTransitionState {
    private val transitions = ConcurrentHashMap<Long, Boolean>()

    fun begin(remoteId: Long, targetCompleted: Boolean) {
        transitions[remoteId] = targetCompleted
    }

    fun end(remoteId: Long) {
        transitions.remove(remoteId)
    }

    fun targetCompleted(remoteId: Long?): Boolean? = remoteId?.let(transitions::get)

    fun isAnimating(remoteId: Long?): Boolean = remoteId != null && transitions.containsKey(remoteId)
}

class StickyNoteWidgetListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        return Factory(applicationContext, widgetId)
    }
}

private class Factory(
    private val context: android.content.Context,
    private val widgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {
    private val todos = mutableListOf<WidgetRenderTodo>()
    private var style: WidgetUiStyle = WidgetStyleResolver.resolveForPreview(
        preset = WidgetStylePreset.PAPER,
        colorTheme = WidgetColorTheme.FOLLOW_APP,
        appAccentTheme = AppAccentTheme.INK_GREEN,
        isDark = false,
        transparency = 0.18f,
        colorfulTextEnabled = true,
    )

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        runCatching {
            runBlocking {
                val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
                val settings = entryPoint.settingsRepository().settings.first()
                style = WidgetStyleResolver.resolve(context, settings)
                val cloudReady = settings.platformEnabled && settings.apiKey.isNotBlank()
                val rawTodos = if (cloudReady) {
                    val store = entryPoint.cloudTodoStore()
                    store.todos.value.ifEmpty { store.snapshotTodos(limit = 200) }
                } else {
                    emptyList()
                }
                val latest = rawTodos
                    .filter { todo ->
                        // 正在播放完成过渡脉冲的条目短暂保留在原列表位置，避免未完成 Tab 下点击瞬间硬闪消失
                        if (WidgetToggleTransitionState.isAnimating(todo.remoteId)) {
                            return@filter true
                        }
                        when (settings.widgetActiveFilter) {
                            DefaultTodoFilter.ALL -> true
                            DefaultTodoFilter.ACTIVE -> !todo.completed
                            DefaultTodoFilter.COMPLETED -> todo.completed
                        }
                    }
                    .take(120)
                    .map { todo ->
                        val animatingTarget = WidgetToggleTransitionState.targetCompleted(todo.remoteId)
                        todo.toRenderTodo(
                            displayCompleted = animatingTarget ?: todo.completed,
                            isAnimating = animatingTarget != null,
                        )
                    }
                todos.clear()
                todos.addAll(latest)
                Log.d(
                    TAG,
                    "onDataSetChanged widgetId=$widgetId count=${latest.size} filter=${settings.widgetActiveFilter} preset=${style.preset}",
                )
            }
        }.onFailure { error ->
            Log.e(TAG, "onDataSetChanged failed widgetId=$widgetId", error)
            todos.clear()
        }
    }

    override fun onDestroy() {
        todos.clear()
    }

    override fun getCount(): Int = todos.size

    override fun getViewAt(position: Int): RemoteViews? {
        val todo = todos.getOrNull(position) ?: return null
        val views = RemoteViews(context.packageName, R.layout.sticky_note_widget_item)
        val radioColor = if (todo.isAnimating) {
            if (todo.completed) style.completedColor else style.accentColor
        } else {
            WidgetStyleResolver.radioColor(style, todo.completed, todo.priority)
        }
        val titleColor = if (todo.isAnimating && todo.completed) {
            style.completedColor
        } else {
            WidgetStyleResolver.todoTextColor(style, todo.completed, todo.priority)
        }
        val rowOpenIntent = Intent().apply {
            putExtra(StickyNoteWidgetReceiver.EXTRA_ITEM_ACTION, StickyNoteWidgetReceiver.ITEM_ACTION_OPEN)
            putExtra(StickyNoteWidgetReceiver.EXTRA_LOCAL_ID, todo.localId)
        }

        // 过渡脉冲高亮底色层（平时隐藏，完成切换瞬时柔和点亮）
        views.setViewVisibility(R.id.widget_item_highlight_bg, if (todo.isAnimating) View.VISIBLE else View.GONE)
        if (todo.isAnimating) {
            val highlightColor = (if (todo.completed) style.completedColor else style.accentColor) or (0xFF shl 24)
            views.setInt(R.id.widget_item_highlight_bg, "setColorFilter", highlightColor)
            views.setFloat(R.id.widget_item_highlight_bg, "setAlpha", if (style.isDark) 0.24f else 0.16f)
        }

        val checkSymbol = when {
            todo.isAnimating && todo.completed -> if (style.useSquareCheckbox) "\u2611" else "\u25CF"
            style.useSquareCheckbox && todo.completed -> "\u2611"
            style.useSquareCheckbox -> "\u2610"
            todo.completed -> "\u2713"
            else -> "\u25CB"
        }
        views.setTextViewText(R.id.widget_item_radio, checkSymbol)
        views.setTextColor(R.id.widget_item_radio, radioColor)
        views.setTextViewText(R.id.widget_item_title, todo.title.withOptionalStrike(todo.completed))
        views.setTextColor(R.id.widget_item_title, titleColor)

        val dueLabel = if (style.showDueDate && !todo.completed) todo.toShortDueLabel() else null
        if (!dueLabel.isNullOrBlank()) {
            val today = LocalDate.now(BEIJING_ZONE_ID)
            val isOverdue = todo.dueDate != null && todo.dueDate.isBefore(today)
            val isToday = todo.dueDate == today
            val dueColor = when {
                (isOverdue || (todo.priority == 2 && isToday)) && style.colorfulTextEnabled -> style.urgentColor
                isToday -> style.accentColor
                else -> style.secondaryTextColor
            }
            views.setViewVisibility(R.id.widget_item_due, View.VISIBLE)
            views.setTextViewText(R.id.widget_item_due, dueLabel)
            views.setTextColor(R.id.widget_item_due, dueColor)
        } else {
            views.setViewVisibility(R.id.widget_item_due, View.GONE)
        }

        val showDivider = style.showItemDivider && position < todos.lastIndex
        views.setViewVisibility(R.id.widget_item_divider, if (showDivider) View.VISIBLE else View.GONE)
        if (showDivider) {
            views.setInt(R.id.widget_item_divider, "setBackgroundColor", style.itemDividerColor)
        }

        views.setOnClickFillInIntent(R.id.widget_item_root, rowOpenIntent)
        views.setOnClickFillInIntent(R.id.widget_item_title, rowOpenIntent)

        if (todo.remoteId != null) {
            views.setOnClickFillInIntent(
                R.id.widget_item_radio,
                Intent().apply {
                    putExtra(StickyNoteWidgetReceiver.EXTRA_ITEM_ACTION, StickyNoteWidgetReceiver.ITEM_ACTION_TOGGLE)
                    putExtra(StickyNoteWidgetReceiver.EXTRA_REMOTE_ID, todo.remoteId)
                },
            )
        }
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        val todo = todos.getOrNull(position) ?: return position.toLong()
        val base = (todo.remoteId ?: todo.localId.hashCode().toLong()) and Long.MAX_VALUE
        val stateBits = (if (todo.completed) 1L else 0L) or (if (todo.isAnimating) 2L else 0L)
        return (base shl 2) xor stateBits
    }

    override fun hasStableIds(): Boolean = true

    private fun Todo.toRenderTodo(
        displayCompleted: Boolean,
        isAnimating: Boolean,
    ): WidgetRenderTodo = WidgetRenderTodo(
        localId = localId,
        remoteId = remoteId,
        title = title,
        completed = displayCompleted,
        isAnimating = isAnimating,
        priority = priority,
        dueDate = dueDate,
        dueTime = dueTime,
    )

    private data class WidgetRenderTodo(
        val localId: String,
        val remoteId: Long?,
        val title: String,
        val completed: Boolean,
        val isAnimating: Boolean,
        val priority: Int,
        val dueDate: LocalDate?,
        val dueTime: LocalTime?,
    ) {
        fun toShortDueLabel(): String? {
            val today = LocalDate.now(BEIJING_ZONE_ID)
            return when {
                dueDate == today && dueTime != null -> dueTime.toString()
                dueDate == today -> "今天"
                dueDate == today.plusDays(1) -> "明天"
                dueDate == today.minusDays(1) -> "昨天"
                dueDate != null -> String.format("%02d-%02d", dueDate.monthValue, dueDate.dayOfMonth)
                dueTime != null -> dueTime.toString()
                else -> null
            }
        }
    }

    private fun String.withOptionalStrike(completed: Boolean): CharSequence {
        if (!completed || isBlank()) return this
        return SpannableString(this).apply {
            setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private companion object {
        val BEIJING_ZONE_ID: ZoneId = ZoneId.of("Asia/Shanghai")
        const val TAG = "StickyNoteWidgetList"
    }
}
