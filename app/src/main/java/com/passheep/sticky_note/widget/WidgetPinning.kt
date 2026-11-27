package com.passheep.sticky_note.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * 桌面小组件一键添加预设尺寸
 */
enum class WidgetPinSize(val label: String) {
    SIZE_2X2("2×2"),
    SIZE_4X2("4×2"),
    SIZE_4X4("4×4"),
}

/**
 * 请求系统桌面固定指定尺寸的便利贴小组件
 */
fun requestPinStickyNoteWidget(
    context: Context,
    size: WidgetPinSize = WidgetPinSize.SIZE_2X2,
): Boolean {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (!appWidgetManager.isRequestPinAppWidgetSupported) {
        return false
    }
    val targetClass = when (size) {
        WidgetPinSize.SIZE_2X2 -> StickyNoteWidgetReceiver::class.java
        WidgetPinSize.SIZE_4X2 -> StickyNoteWidget4x2Receiver::class.java
        WidgetPinSize.SIZE_4X4 -> StickyNoteWidget4x4Receiver::class.java
    }
    return appWidgetManager.requestPinAppWidget(
        ComponentName(context, targetClass),
        null,
        null,
    )
}
