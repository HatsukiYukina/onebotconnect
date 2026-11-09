//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Assisted-by: Deepseek:deepseekv4.1-flash-1017
//该代码使用了llm辅助，按必要的披露责任将使用的llm列出
//This code use LLM assistance and list the LLM used according to necessary disclosure responsibilities.
//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.client

import net.nyauru.onebotwslib.adapter.event.Anonymous
import net.nyauru.onebotwslib.adapter.event.BotStatus
import net.nyauru.onebotwslib.adapter.event.FileInfo
import net.nyauru.onebotwslib.adapter.event.GroupMessageEvent
import net.nyauru.onebotwslib.adapter.event.MetaEvent
import net.nyauru.onebotwslib.adapter.event.MessageEvent
import net.nyauru.onebotwslib.adapter.event.NoticeEvent
import net.nyauru.onebotwslib.adapter.event.OneBotEvent
import net.nyauru.onebotwslib.adapter.event.PrivateMessageEvent
import net.nyauru.onebotwslib.adapter.event.RequestEvent
import net.nyauru.onebotwslib.adapter.event.Sender
import net.nyauru.onebotwslib.adapter.event.UnknownOneBotEvent
import net.nyauru.onebotwslib.adapter.message.CQCode
import net.nyauru.onebotwslib.adapter.message.MessageSegment
import net.nyauru.onebotwslib.adapter.message.toCQString
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser


internal object OneBotJson {
    val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

    //归类
    sealed interface IncomingFrame {
        data class EventFrame(val event: OneBotEvent) : IncomingFrame
        data class ResponseFrame(val response: ActionResponse) : IncomingFrame
        data class Invalid(val error: OneBotProtocolException) : IncomingFrame
    }

    //构造请求帧
    fun actionFrame(action: String, params: Map<String, Any?>, echo: String): String {
        val root = JsonObject()
        root.addProperty("action", action)
        root.add("params", gson.toJsonTree(params))
        root.addProperty("echo", echo)
        return gson.toJson(root)
    }

    //统一消息段数组发
    fun messageArray(message: List<MessageSegment>): JsonArray {
        val array = JsonArray()
        for (segment in message) {
            val item = JsonObject()
            item.addProperty("type", segment.type)
            item.add("data", gson.toJsonTree(segment.data))
            array.add(item)
        }
        return array
    }


    fun parseIncoming(raw: String): IncomingFrame {
        val root = try {
            JsonParser.parseString(raw)
        } catch (t: Throwable) {
            return IncomingFrame.Invalid(OneBotProtocolException("invalid JSON frame", t))
        }
        if (!root.isJsonObject) {
            return IncomingFrame.Invalid(OneBotProtocolException("frame is not a JSON object"))
        }

        val obj = root.asJsonObject
        return try {
            when {
                obj.has("post_type") -> IncomingFrame.EventFrame(parseEvent(obj, raw))
                obj.has("retcode") || obj.has("status") -> IncomingFrame.ResponseFrame(parseResponse(obj, raw))
                else -> IncomingFrame.Invalid(OneBotProtocolException("unrecognized frame"))
            }
        } catch (t: Throwable) {
            IncomingFrame.Invalid(OneBotProtocolException("failed to parse frame", t))
        }
    }

    private fun parseEvent(obj: JsonObject, raw: String): OneBotEvent {
        val time = obj.longOrNull("time") ?: 0L
        val selfId = obj.longOrNull("self_id") ?: 0L
        val postType = obj.stringOrNull("post_type").orEmpty()

        return when (postType) {
            "message", "message_sent" -> parseMessage(obj, raw, time, selfId, postType)
            "notice" -> parseNotice(obj, raw, time, selfId)
            "request" -> parseRequest(obj, raw, time, selfId)
            "meta_event" -> parseMetaEvent(obj, raw, time, selfId)
            else -> UnknownOneBotEvent(
                time = time,
                selfId = selfId,
                postType = postType,
                fields = obj.toMap(),
                rawJson = raw,
            )
        }
    }

    private fun parseMessage(
        obj: JsonObject,
        raw: String,
        time: Long,
        selfId: Long,
        postType: String,
    ): MessageEvent {
        val messageType = obj.stringOrNull("message_type") ?: if (obj.has("group_id")) "group" else "private"
        val subType = obj.stringOrNull("sub_type")
        val messageId = obj.longOrNull("message_id")
        val userId = obj.longOrNull("user_id") ?: 0L
        val message = normalizeMessage(obj)
        val rawMessage = obj.stringOrNull("raw_message") ?: message.toCQString()
        val font = obj.longOrNull("font")
        val sender = obj.objectOrNull("sender")?.toSender()
        val sentBySelf = postType == "message_sent"
        val targetId = obj.longOrNull("target_id")

        return if (messageType == "group") {
            GroupMessageEvent(
                time = time,
                selfId = selfId,
                groupId = obj.longOrNull("group_id") ?: 0L,
                subType = subType,
                messageId = messageId,
                userId = userId,
                message = message,
                rawMessage = rawMessage,
                font = font,
                sender = sender,
                anonymous = obj.objectOrNull("anonymous")?.toAnonymous(),
                sentBySelf = sentBySelf,
                targetId = targetId,
                rawJson = raw,
            )
        } else {
            PrivateMessageEvent(
                time = time,
                selfId = selfId,
                subType = subType,
                messageId = messageId,
                userId = userId,
                message = message,
                rawMessage = rawMessage,
                font = font,
                sender = sender,
                sentBySelf = sentBySelf,
                targetId = targetId,
                rawJson = raw,
            )
        }
    }

    //
    private fun normalizeMessage(obj: JsonObject): List<MessageSegment> {
        val element = obj.get("message") ?: return emptyList()
        return when {
            element.isJsonNull -> emptyList()
            element.isJsonArray -> element.asJsonArray.mapNotNull { segmentFromJson(it) }
            element.isJsonPrimitive && element.asJsonPrimitive.isString -> CQCode.parse(element.asString)
            else -> emptyList()
        }
    }

    private fun segmentFromJson(element: JsonElement): MessageSegment? {
        if (!element.isJsonObject) return null
        val obj = element.asJsonObject
        val type = obj.stringOrNull("type") ?: return null
        if (type.isBlank()) return null

        val data = LinkedHashMap<String, String>()
        obj.objectOrNull("data")?.entrySet()?.forEach { (key, value) ->
            valueAsString(value)?.let { data[key] = it }
        }
        return MessageSegment(type, data)
    }

    private fun parseNotice(obj: JsonObject, raw: String, time: Long, selfId: Long): NoticeEvent =
        NoticeEvent(
            time = time,
            selfId = selfId,
            noticeType = obj.stringOrNull("notice_type").orEmpty(),
            subType = obj.stringOrNull("sub_type"),
            groupId = obj.longOrNull("group_id"),
            userId = obj.longOrNull("user_id"),
            operatorId = obj.longOrNull("operator_id"),
            targetId = obj.longOrNull("target_id"),
            messageId = obj.longOrNull("message_id"),
            duration = obj.longOrNull("duration"),
            file = obj.objectOrNull("file")?.toFileInfo(),
            fields = obj.toMap(),
            rawJson = raw,
        )

    private fun parseRequest(obj: JsonObject, raw: String, time: Long, selfId: Long): RequestEvent =
        RequestEvent(
            time = time,
            selfId = selfId,
            requestType = obj.stringOrNull("request_type").orEmpty(),
            subType = obj.stringOrNull("sub_type"),
            flag = obj.stringOrNull("flag"),
            comment = obj.stringOrNull("comment"),
            groupId = obj.longOrNull("group_id"),
            userId = obj.longOrNull("user_id"),
            fields = obj.toMap(),
            rawJson = raw,
        )

    private fun parseMetaEvent(obj: JsonObject, raw: String, time: Long, selfId: Long): MetaEvent =
        MetaEvent(
            time = time,
            selfId = selfId,
            metaEventType = obj.stringOrNull("meta_event_type").orEmpty(),
            subType = obj.stringOrNull("sub_type"),
            interval = obj.longOrNull("interval"),
            status = obj.objectOrNull("status")?.let { status ->
                BotStatus(
                    online = status.booleanOrNull("online"),
                    good = status.booleanOrNull("good"),
                    extra = status.toMap(),
                )
            },
            fields = obj.toMap(),
            rawJson = raw,
        )

    private fun parseResponse(obj: JsonObject, raw: String): ActionResponse =
        ActionResponse(
            status = obj.stringOrNull("status") ?: "unknown",
            retcode = obj.intOrNull("retcode") ?: -1,
            data = obj.get("data")?.toActionData(),
            echo = obj.stringOrNull("echo"),
            message = obj.stringOrNull("message"),
            rawJson = raw,
        )

    private fun JsonElement.toActionData(): ActionData? = when {
        isJsonNull -> null
        isJsonObject -> ActionData.ObjectData(asJsonObject.toMap())
        isJsonArray -> ActionData.ArrayData(asJsonArray.map { it.toAny() })
        else -> ActionData.ScalarData(toAny())
    }

    private fun JsonObject.toSender(): Sender =
        Sender(
            userId = longOrNull("user_id"),
            nickname = stringOrNull("nickname"),
            card = stringOrNull("card"),
            role = stringOrNull("role"),
            title = stringOrNull("title"),
            sex = stringOrNull("sex"),
            age = intOrNull("age"),
            area = stringOrNull("area"),
            level = stringOrNull("level"),
        )

    private fun JsonObject.toAnonymous(): Anonymous =
        Anonymous(
            id = longOrNull("id"),
            name = stringOrNull("name"),
            flag = stringOrNull("flag"),
        )

    private fun JsonObject.toFileInfo(): FileInfo =
        FileInfo(
            id = stringOrNull("id"),
            name = stringOrNull("name"),
            size = longOrNull("size"),
            busid = intOrNull("busid"),
            url = stringOrNull("url"),
            file = stringOrNull("file"),
        )

    private fun JsonObject.toMap(): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        for ((key, value) in entrySet()) {
            result[key] = value.toAny()
        }
        return result
    }

    //整数转long
    private fun JsonElement.toAny(): Any? = when {
        isJsonNull -> null
        isJsonObject -> asJsonObject.toMap()
        isJsonArray -> asJsonArray.map { it.toAny() }
        isJsonPrimitive -> asJsonPrimitive.let { primitive ->
            when {
                primitive.isBoolean -> primitive.asBoolean
                primitive.isNumber -> primitive.asString.toLongOrNull()
                    ?: primitive.asString.toDoubleOrNull()
                    ?: primitive.asNumber
                else -> primitive.asString
            }
        }
        else -> null
    }

    private fun valueAsString(element: JsonElement): String? = when {
        element.isJsonNull -> null
        element.isJsonPrimitive -> element.asString
        else -> gson.toJson(element)
    }

    private fun JsonObject.objectOrNull(key: String): JsonObject? =
        get(key)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.stringOrNull(key: String): String? {
        val element = get(key) ?: return null
        return if (element.isJsonNull) null else if (element.isJsonPrimitive) element.asString else null
    }

    private fun JsonObject.longOrNull(key: String): Long? {
        val element = get(key) ?: return null
        if (element.isJsonNull || !element.isJsonPrimitive) return null
        val primitive = element.asJsonPrimitive
        return when {
            primitive.isNumber -> primitive.asLong
            primitive.isString -> primitive.asString.toLongOrNull()
            else -> null
        }
    }

    private fun JsonObject.intOrNull(key: String): Int? = longOrNull(key)?.toInt()

    private fun JsonObject.booleanOrNull(key: String): Boolean? {
        val element = get(key) ?: return null
        if (element.isJsonNull || !element.isJsonPrimitive) return null
        val primitive = element.asJsonPrimitive
        return when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isString -> primitive.asString.toBooleanStrictOrNull()
            else -> null
        }
    }
}
