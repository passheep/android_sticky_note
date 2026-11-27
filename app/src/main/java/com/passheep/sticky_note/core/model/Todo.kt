package com.passheep.sticky_note.core.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalTime

@Immutable
data class Todo(
    val localId: String,
    val remoteId: Long?,
    val title: String,
    val description: String,
    val dueDate: LocalDate?,
    val dueTime: LocalTime?,
    val repeatType: RepeatType,
    val repeatWeekday: Int?,
    val repeatMonth: Int?,
    val repeatDay: Int?,
    val priority: Int,
    val status: TodoStatus,
    val completed: Boolean,
    val deviceId: String?,
    val deviceName: String?,
    val syncState: SyncState,
    val pendingAction: SyncAction?,
    val conflictState: ConflictState,
    val localUpdatedAt: Long,
    val remoteUpdatedAt: Long?,
    val createdAt: Long,
    val remoteCreateDate: String?,
    val isDeletedLocally: Boolean,
    val isDeletedRemotely: Boolean,
    val lastSyncError: String?,
)

enum class RepeatType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

enum class TodoStatus {
    TODO,
    COMPLETED,
}

enum class SyncState {
    SYNCED,
    PENDING,
    FAILED,
    CONFLICT,
}

enum class ConflictState {
    NONE,
    REMOTE_DELETED,
}

