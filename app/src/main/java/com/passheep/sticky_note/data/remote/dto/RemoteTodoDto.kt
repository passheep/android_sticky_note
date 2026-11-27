package com.passheep.sticky_note.data.remote.dto

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * 宽容整数反序列化器：兼容数字、数字字符串、空串与 null，防止服务端返回 null/空串时抛 JSON 解析异常
 */
object FlexibleNullableIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleNullableInt", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Int? {
        val jsonDecoder = decoder as? JsonDecoder ?: return runCatching { decoder.decodeInt() }.getOrNull()
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive ?: return null
        return primitive.intOrNull ?: primitive.content.trim().toIntOrNull()
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
    }
}

/**
 * 宽容布尔反序列化器：兼容 true/false、0/1 与 null
 */
object FlexibleNullableBooleanSerializer : KSerializer<Boolean?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleNullableBoolean", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Boolean? {
        val jsonDecoder = decoder as? JsonDecoder ?: return runCatching { decoder.decodeBoolean() }.getOrNull()
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive ?: return null
        return primitive.booleanOrNull
            ?: primitive.intOrNull?.let { it != 0 }
            ?: primitive.content.trim().lowercase().let {
                when (it) {
                    "true", "1" -> true
                    "false", "0" -> false
                    else -> null
                }
            }
    }

    override fun serialize(encoder: Encoder, value: Boolean?) {
        if (value == null) encoder.encodeNull() else encoder.encodeBoolean(value)
    }
}

/**
 * 宽容时间戳反序列化器：兼容秒/毫秒数字时间戳、日期时间字符串与 null
 */
object FlexibleNullableTimestampSerializer : KSerializer<Long?> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val beijingZone: ZoneId = ZoneId.of("Asia/Shanghai")

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleNullableTimestamp", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Long? {
        val jsonDecoder = decoder as? JsonDecoder ?: return runCatching { decoder.decodeLong() }.getOrNull()
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive ?: return null
        primitive.longOrNull?.let { return it }
        val text = primitive.content.trim()
        if (text.isEmpty() || text.equals("null", ignoreCase = true)) return null
        text.toLongOrNull()?.let { return it }
        return runCatching {
            LocalDateTime.parse(text.replace('T', ' ').take(19), formatter)
                .atZone(beijingZone)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    override fun serialize(encoder: Encoder, value: Long?) {
        if (value == null) encoder.encodeNull() else encoder.encodeLong(value)
    }
}

@Serializable
data class RemoteTodoDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String = "",
    @SerialName("description") val description: String? = null,
    @SerialName("dueDate") val dueDate: String? = null,
    @SerialName("dueTime") val dueTime: String? = null,
    @SerialName("repeatType") val repeatType: String? = null,
    @Serializable(with = FlexibleNullableIntSerializer::class)
    @SerialName("repeatWeekday") val repeatWeekday: Int? = null,
    @Serializable(with = FlexibleNullableIntSerializer::class)
    @SerialName("repeatMonth") val repeatMonth: Int? = null,
    @Serializable(with = FlexibleNullableIntSerializer::class)
    @SerialName("repeatDay") val repeatDay: Int? = null,
    @Serializable(with = FlexibleNullableIntSerializer::class)
    @SerialName("status") val status: Int? = 0,
    @Serializable(with = FlexibleNullableIntSerializer::class)
    @SerialName("priority") val priority: Int? = 0,
    @Serializable(with = FlexibleNullableBooleanSerializer::class)
    @SerialName("completed") val completed: Boolean? = false,
    @SerialName("deviceId") val deviceId: String? = null,
    @SerialName("deviceName") val deviceName: String? = null,
    @SerialName("createDate") val createDate: String? = null,
    @Serializable(with = FlexibleNullableTimestampSerializer::class)
    @SerialName("updateDate") val updateDate: Long? = null,
)
