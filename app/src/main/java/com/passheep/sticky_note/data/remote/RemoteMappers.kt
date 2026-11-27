package com.passheep.sticky_note.data.remote

import com.passheep.sticky_note.core.model.ConflictState
import com.passheep.sticky_note.core.model.Device
import com.passheep.sticky_note.core.model.RepeatType
import com.passheep.sticky_note.core.model.SyncState
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.model.TodoStatus
import com.passheep.sticky_note.data.remote.dto.CreateTodoRequest
import com.passheep.sticky_note.data.remote.dto.DeviceDto
import com.passheep.sticky_note.data.remote.dto.RemoteTodoDto
import com.passheep.sticky_note.data.remote.dto.UpdateTodoRequest
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

fun DeviceDto.toModel(selectedDeviceId: String?, now: Long): Device = Device(
    deviceId = deviceId,
    alias = alias.orEmpty(),
    board = board.orEmpty(),
    isSelected = deviceId == selectedDeviceId,
    syncedAt = now,
)

fun RemoteTodoDto.toModel(
    existing: Todo? = null,
    now: Long = System.currentTimeMillis(),
): Todo {
    val remoteUpdatedAtMillis = updateDate.toEpochMillis() ?: now
    val statusModel = if (completed == true || status == 1) TodoStatus.COMPLETED else TodoStatus.TODO
    return Todo(
        localId = existing?.localId ?: UUID.randomUUID().toString(),
        remoteId = id,
        title = title,
        description = description.orEmpty(),
        dueDate = dueDate?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        dueTime = dueTime?.takeIf { it.isNotBlank() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        repeatType = repeatType.orEmpty().toRepeatType(),
        repeatWeekday = repeatWeekday,
        repeatMonth = repeatMonth,
        repeatDay = repeatDay,
        priority = (priority ?: 0).coerceIn(0, 2),
        status = statusModel,
        completed = statusModel == TodoStatus.COMPLETED,
        deviceId = deviceId,
        deviceName = deviceName,
        syncState = SyncState.SYNCED,
        pendingAction = null,
        conflictState = ConflictState.NONE,
        localUpdatedAt = remoteUpdatedAtMillis,
        remoteUpdatedAt = remoteUpdatedAtMillis,
        createdAt = existing?.createdAt ?: remoteUpdatedAtMillis,
        remoteCreateDate = createDate,
        isDeletedLocally = false,
        isDeletedRemotely = false,
        lastSyncError = null,
    )
}

fun Todo.toCreateRequest(): CreateTodoRequest = CreateTodoRequest(
    title = title,
    description = description.takeIf { it.isNotBlank() },
    dueDate = dueDate?.toString(),
    dueTime = dueTime?.toString(),
    repeatType = repeatType.toApiValue(),
    repeatWeekday = if (repeatType == RepeatType.WEEKLY) repeatWeekday else null,
    repeatMonth = if (repeatType == RepeatType.YEARLY) repeatMonth else null,
    repeatDay = when (repeatType) {
        RepeatType.MONTHLY, RepeatType.YEARLY -> repeatDay
        else -> null
    },
    priority = priority.coerceIn(0, 2),
    deviceId = deviceId?.takeIf { it.isNotBlank() },
)

fun Todo.toUpdateRequest(): UpdateTodoRequest = UpdateTodoRequest(
    title = title,
    description = description,
    dueDate = dueDate?.toString(),
    dueTime = dueTime?.toString(),
    repeatType = repeatType.toApiValue(),
    repeatWeekday = if (repeatType == RepeatType.WEEKLY) repeatWeekday else null,
    repeatMonth = if (repeatType == RepeatType.YEARLY) repeatMonth else null,
    repeatDay = when (repeatType) {
        RepeatType.MONTHLY, RepeatType.YEARLY -> repeatDay
        else -> null
    },
    priority = priority.coerceIn(0, 2),
    deviceId = deviceId?.takeIf { it.isNotBlank() },
)

fun Long?.toEpochMillis(): Long? {
    val raw = this ?: return null
    return if (raw < 100_000_000_000L) raw * 1000L else raw
}

private fun String.toRepeatType(): RepeatType = when (lowercase()) {
    "daily" -> RepeatType.DAILY
    "weekly" -> RepeatType.WEEKLY
    "monthly" -> RepeatType.MONTHLY
    "yearly" -> RepeatType.YEARLY
    else -> RepeatType.NONE
}

private fun RepeatType.toApiValue(): String = when (this) {
    RepeatType.NONE -> "none"
    RepeatType.DAILY -> "daily"
    RepeatType.WEEKLY -> "weekly"
    RepeatType.MONTHLY -> "monthly"
    RepeatType.YEARLY -> "yearly"
}
