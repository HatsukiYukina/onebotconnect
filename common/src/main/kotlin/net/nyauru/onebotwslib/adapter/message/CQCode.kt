//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Assisted-by: Qwen:qwen3.8-flash-next
//该代码使用了llm辅助，按必要的披露责任将使用的llm列出
//This code use LLM assistance and list the LLM used according to necessary disclosure responsibilities.
//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.message

///cq码解析与序列化
internal object CQCode {
    private const val PREFIX = "[CQ:"
    private const val AMP = "&amp;"
    private const val LEFT = "&#91;"
    private const val RIGHT = "&#93;"
    private const val COMMA = "&#44;"

    fun parse(input: String): List<MessageSegment> {
        if (input.isEmpty()) return emptyList()

        val result = ArrayList<MessageSegment>()
        var index = 0
        while (index < input.length) {
            val start = input.indexOf(PREFIX, index)
            if (start < 0) {
                appendText(result, unescape(input.substring(index)))
                break
            }
            if (start > index) {
                appendText(result, unescape(input.substring(index, start)))
            }

            val end = input.indexOf(']', start + PREFIX.length)
            if (end < 0) {
                appendText(result, unescape(input.substring(start)))
                break
            }

            val body = input.substring(start + PREFIX.length, end)
            val segment = parseBody(body)
            if (segment == null) {
                appendText(result, unescape(input.substring(start, end + 1)))
            } else {
                result.add(segment)
            }
            index = end + 1
        }
        return result
    }

    fun serialize(segments: List<MessageSegment>): String =
        segments.joinToString(separator = "") { serializeSegment(it) }

    fun serializeSegment(segment: MessageSegment): String {
        if (segment.type == "text") {
            return escapeText(segment.text.orEmpty())
        }
        return buildString {
            append(PREFIX)
            append(segment.type)
            for ((key, value) in segment.data) {
                append(',')
                append(escapeParam(key))
                append('=')
                append(escapeParam(value))
            }
            append(']')
        }
    }

    fun escapeText(value: String): String = escapeBase(value)

    fun escapeParam(value: String): String = escapeBase(value).replace(",", COMMA)

    fun unescape(value: String): String =
        value
            .replace(COMMA, ",")
            .replace(LEFT, "[")
            .replace(RIGHT, "]")
            .replace(AMP, "&")

    private fun escapeBase(value: String): String =
        value
            .replace("&", AMP)
            .replace("[", LEFT)
            .replace("]", RIGHT)

    private fun parseBody(body: String): MessageSegment? {
        if (body.isEmpty()) return null
        val parts = body.split(',')
        val type = parts[0]
        if (type.isBlank()) return null

        val data = LinkedHashMap<String, String>()
        for (i in 1 until parts.size) {
            val part = parts[i]
            val equals = part.indexOf('=')
            if (equals < 0) {
                data[unescape(part)] = ""
            } else {
                data[unescape(part.substring(0, equals))] = unescape(part.substring(equals + 1))
            }
        }
        return MessageSegment(type, data)
    }

    private fun appendText(target: MutableList<MessageSegment>, text: String) {
        if (text.isNotEmpty()) target.add(MessageSegment.text(text))
    }
}
