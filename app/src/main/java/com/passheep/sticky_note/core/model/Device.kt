package com.passheep.sticky_note.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Device(
    val deviceId: String,
    val alias: String,
    val board: String,
    val isSelected: Boolean,
    val syncedAt: Long,
)

