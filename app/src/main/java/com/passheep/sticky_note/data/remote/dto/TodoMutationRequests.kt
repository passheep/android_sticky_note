package com.passheep.sticky_note.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 创建待办请求体，字段类型严格对齐极趣云 Open API（repeatWeekday/Month/Day 为整型，可选字段为 null 时不发送）
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CreateTodoRequest(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("dueDate") val dueDate: String? = null,
    @SerialName("dueTime") val dueTime: String? = null,
    @SerialName("repeatType") val repeatType: String? = null,
    @SerialName("repeatWeekday") val repeatWeekday: Int? = null,
    @SerialName("repeatMonth") val repeatMonth: Int? = null,
    @SerialName("repeatDay") val repeatDay: Int? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("priority") val priority: Int = 0,
    @SerialName("deviceId") val deviceId: String? = null,
)

/**
 * 更新待办请求体，可选字段为 null 时省略发送，避免空字符串触发后端反序列化/校验失败
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UpdateTodoRequest(
    @SerialName("title") val title: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("description") val description: String = "",
    @SerialName("dueDate") val dueDate: String? = null,
    @SerialName("dueTime") val dueTime: String? = null,
    @SerialName("repeatType") val repeatType: String? = null,
    @SerialName("repeatWeekday") val repeatWeekday: Int? = null,
    @SerialName("repeatMonth") val repeatMonth: Int? = null,
    @SerialName("repeatDay") val repeatDay: Int? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("priority") val priority: Int = 0,
    @SerialName("deviceId") val deviceId: String? = null,
)

/**
 * 修改设备同步周期请求体（官网新增 PUT /open/v1/devices/{deviceId}/sync-interval）
 */
@Serializable
data class UpdateDeviceSyncIntervalRequest(
    @SerialName("syncInterval") val syncInterval: Int,
)

@Serializable
data class DeviceSyncIntervalResponse(
    @SerialName("deviceId") val deviceId: String? = null,
    @SerialName("syncInterval") val syncInterval: Int? = null,
)
