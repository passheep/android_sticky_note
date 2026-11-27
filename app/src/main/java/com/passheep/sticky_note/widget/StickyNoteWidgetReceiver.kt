package com.passheep.sticky_note.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.passheep.sticky_note.MainActivity
import com.passheep.sticky_note.core.settings.DefaultTodoFilter
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class StickyNoteWidgetReceiver : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "onEnabled enqueue refresh")
        WidgetActionWorker.enqueueRefresh(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "onUpdate enqueue refresh ids=${appWidgetIds.size}")
        WidgetActionWorker.enqueueRefresh(context)
    }

    /**
     * 桌面拖拽缩放小组件尺寸后立即重新渲染适配
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Log.d(TAG, "onAppWidgetOptionsChanged widgetId=$appWidgetId")
        WidgetActionWorker.enqueueRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_REFRESH -> {
                Log.d(TAG, "onReceive refresh")
                WidgetActionWorker.enqueueRefresh(context)
            }

            ACTION_WIDGET_FILTER -> {
                val filterName = intent.getStringExtra(EXTRA_FILTER)
                val targetFilter = DefaultTodoFilter.entries.firstOrNull { it.name == filterName } ?: return
                val pendingResult = goAsync()
                val appContext = context.applicationContext
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val entryPoint = EntryPointAccessors.fromApplication(appContext, WidgetEntryPoint::class.java)
                        entryPoint.settingsRepository().updateWidgetActiveFilter(targetFilter)
                        StickyNoteRemoteWidgetRenderer.refreshAllFromCloud(appContext)
                    } catch (error: Throwable) {
                        Log.e(TAG, "onReceive filter switch failed filter=$targetFilter", error)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_WIDGET_TOGGLE -> {
                val remoteId = intent.getLongExtra(EXTRA_REMOTE_ID, -1L)
                Log.d(TAG, "onReceive toggle remoteId=$remoteId")
                if (remoteId > 0L) {
                    triggerAnimatedToggle(context, remoteId)
                }
            }

            ACTION_WIDGET_ITEM -> {
                when (intent.getStringExtra(EXTRA_ITEM_ACTION)) {
                    ITEM_ACTION_TOGGLE -> {
                        val remoteId = intent.getLongExtra(EXTRA_REMOTE_ID, -1L)
                        Log.d(TAG, "onReceive item toggle remoteId=$remoteId")
                        if (remoteId > 0L) {
                            triggerAnimatedToggle(context, remoteId)
                        }
                    }

                    ITEM_ACTION_OPEN -> {
                        val localId = intent.getStringExtra(EXTRA_LOCAL_ID).orEmpty()
                        if (localId.isBlank()) return
                        Log.d(TAG, "onReceive item open localId=$localId")
                        context.startActivity(
                            Intent(context, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra(MainActivity.EXTRA_OPEN_TODO_ID, localId)
                            },
                        )
                    }
                }
            }

            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_USER_UNLOCKED,
            Intent.ACTION_SCREEN_ON,
            -> {
                if (hasWidgetInstances(context) && shouldTriggerForegroundRefresh()) {
                    Log.d(TAG, "onReceive foreground signal action=${intent.action} enqueue refresh")
                    WidgetActionWorker.enqueueRefresh(context)
                }
            }
        }
    }

    /**
     * 两阶段桌面小组件完成动效：
     * 0ms 立即展示柔和脉冲高亮与实心勾选反馈，280ms 后落定最终态并同步云端（失败时交由 WorkManager 兜底重试）
     */
    private fun triggerAnimatedToggle(context: Context, remoteId: Long) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(appContext, WidgetEntryPoint::class.java)
                val store = entryPoint.cloudTodoStore()
                val currentCompleted = store.todos.value.firstOrNull { it.remoteId == remoteId }?.completed
                val targetCompleted = currentCompleted?.not() ?: true

                // 阶段 1：0ms 立即展示完成/取消完成高亮脉冲反馈
                WidgetToggleTransitionState.begin(remoteId, targetCompleted)
                StickyNoteRemoteWidgetRenderer.refreshAllFromCloud(appContext)

                // 并行启动云端切换（包含内存乐观更新），同时确保用户肉眼看清约 280ms 的高亮过渡动画
                val toggleJob = launch {
                    runCatching { store.toggleCompleted(remoteId) }
                        .onFailure { error ->
                            Log.w(TAG, "triggerAnimatedToggle direct api failed, fallback to worker", error)
                            WidgetActionWorker.enqueueToggle(appContext, remoteId)
                        }
                }
                delay(280)

                // 阶段 2：撤除过渡脉冲，自然过渡到最终态（划线或从未完成列表收起）
                WidgetToggleTransitionState.end(remoteId)
                StickyNoteRemoteWidgetRenderer.refreshAllFromCloud(appContext)
                toggleJob.join()
            } catch (error: Throwable) {
                WidgetToggleTransitionState.end(remoteId)
                Log.e(TAG, "triggerAnimatedToggle failed remoteId=$remoteId", error)
                WidgetActionWorker.enqueueToggle(appContext, remoteId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun hasWidgetInstances(context: Context): Boolean = allWidgetIds(context).isNotEmpty()

    private fun shouldTriggerForegroundRefresh(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastForegroundSignalRefreshAt < FOREGROUND_SIGNAL_REFRESH_GAP_MILLIS) {
            return false
        }
        lastForegroundSignalRefreshAt = now
        return true
    }

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.passheep.sticky_note.widget.ACTION_WIDGET_REFRESH"
        const val ACTION_WIDGET_FILTER = "com.passheep.sticky_note.widget.ACTION_WIDGET_FILTER"
        const val ACTION_WIDGET_TOGGLE = "com.passheep.sticky_note.widget.ACTION_WIDGET_TOGGLE"
        const val ACTION_WIDGET_ITEM = "com.passheep.sticky_note.widget.ACTION_WIDGET_ITEM"
        const val EXTRA_FILTER = "extra_filter"
        const val EXTRA_ITEM_ACTION = "extra_item_action"
        const val EXTRA_REMOTE_ID = "extra_remote_id"
        const val EXTRA_LOCAL_ID = "extra_local_id"
        const val ITEM_ACTION_TOGGLE = "toggle"
        const val ITEM_ACTION_OPEN = "open"
        private const val FOREGROUND_SIGNAL_REFRESH_GAP_MILLIS = 20_000L
        @Volatile private var lastForegroundSignalRefreshAt: Long = 0L
        private const val TAG = "StickyNoteWidgetRcvr"

        /**
         * 收集全部尺寸入口（2x2 / 4x2 / 4x4）的小组件实例 ID
         */
        fun allWidgetIds(context: Context): IntArray {
            val manager = AppWidgetManager.getInstance(context)
            val providers = listOf(
                StickyNoteWidgetReceiver::class.java,
                StickyNoteWidget4x2Receiver::class.java,
                StickyNoteWidget4x4Receiver::class.java,
            )
            return providers
                .flatMap { clazz -> manager.getAppWidgetIds(ComponentName(context, clazz)).toList() }
                .distinct()
                .toIntArray()
        }
    }
}

/**
 * 4×2 宽屏规格桌面小组件入口（添加后仍可在桌面自由缩放）
 */
class StickyNoteWidget4x2Receiver : StickyNoteWidgetReceiver()

/**
 * 4×4 大号清单规格桌面小组件入口（添加后仍可在桌面自由缩放）
 */
class StickyNoteWidget4x4Receiver : StickyNoteWidgetReceiver()
