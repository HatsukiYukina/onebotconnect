//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.message

//obv11消息段
data class MessageSegment(
    val type: String,
    val data: Map<String, String> = emptyMap(),
) {
    init {
        require(type.isNotBlank()) { "segment type must not be blank" }
    }

    val text: String? get() = data["text"]
    val file: String? get() = data["file"]
    val url: String? get() = data["url"]
    val qq: String? get() = data["qq"]
    val id: String? get() = data["id"]
    val messageId: Long? get() = data["id"]?.toLongOrNull()
    val subType: Int? get() = data["sub_type"]?.toIntOrNull()

    fun param(key: String): String? = data[key]

    fun toCQCode(): String = CQCode.serializeSegment(this)

    companion object {
        @JvmStatic
        fun text(value: String): MessageSegment =
            MessageSegment("text", mapOf("text" to value))

        @JvmStatic
        fun at(userId: Long): MessageSegment =
            MessageSegment("at", mapOf("qq" to userId.toString()))

        @JvmStatic
        fun reply(messageId: Long): MessageSegment =
            MessageSegment("reply", mapOf("id" to messageId.toString()))

        @JvmStatic
        fun image(file: String): MessageSegment =
            MessageSegment("image", mapOf("file" to file))

        @JvmStatic
        fun record(file: String): MessageSegment =
            MessageSegment("record", mapOf("file" to file))

        @JvmStatic
        fun video(file: String): MessageSegment =
            MessageSegment("video", mapOf("file" to file))

        @JvmStatic
        fun face(id: Int): MessageSegment =
            MessageSegment("face", mapOf("id" to id.toString()))

        @JvmStatic
        fun parseCQ(cq: String): List<MessageSegment> = CQCode.parse(cq)
    }
}

fun String.toCQSegments(): List<MessageSegment> = CQCode.parse(this)

fun List<MessageSegment>.toCQString(): String = CQCode.serialize(this)

val List<MessageSegment>.plainText: String
    get() = joinToString("") { segment -> if (segment.type == "text") segment.text.orEmpty() else "" }
