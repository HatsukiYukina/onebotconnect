//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Assisted-by: Deepseek:deepseekv4.1-flash-1017
//该代码使用了llm辅助，按必要的披露责任将使用的llm列出
//This code use LLM assistance and list the LLM used according to necessary disclosure responsibilities.
//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.event

import net.nyauru.onebotwslib.adapter.message.MessageSegment
import net.nyauru.onebotwslib.adapter.message.plainText

//私聊群聊消息事件
//message统一为消息段列表
//原始文本从rawMessage获取
sealed class MessageEvent : OneBotEvent() {
    abstract val messageType: String
    abstract val subType: String?
    abstract val messageId: Long?
    abstract val userId: Long
    abstract val message: List<MessageSegment>
    abstract val rawMessage: String
    abstract val font: Long?
    abstract val sender: Sender?
    abstract val sentBySelf: Boolean
    abstract val targetId: Long?

    //仅拼接text段
    val plainText: String get() = message.plainText

    val replyId: Long?
        get() = message.firstOrNull { it.type == "reply" }?.messageId

    fun atTargets(): List<Long> =
        message.filter { it.type == "at" }.mapNotNull { it.qq?.toLongOrNull() }

    fun mentions(userId: Long): Boolean = atTargets().contains(userId)
}

data class PrivateMessageEvent(
    override val time: Long,
    override val selfId: Long,
    override val subType: String? = null,
    override val messageId: Long? = null,
    override val userId: Long,
    override val message: List<MessageSegment> = emptyList(),
    override val rawMessage: String = "",
    override val font: Long? = null,
    override val sender: Sender? = null,
    override val sentBySelf: Boolean = false,
    override val targetId: Long? = null,
    override val rawJson: String = "",
) : MessageEvent() {
    override val messageType: String get() = "private"

    //分辨自己发的信息
    override val postType: String get() = if (sentBySelf) "message_sent" else "message"
}

data class GroupMessageEvent(
    override val time: Long,
    override val selfId: Long,
    val groupId: Long,
    override val subType: String? = null,
    override val messageId: Long? = null,
    override val userId: Long,
    override val message: List<MessageSegment> = emptyList(),
    override val rawMessage: String = "",
    override val font: Long? = null,
    override val sender: Sender? = null,
    val anonymous: Anonymous? = null,
    override val sentBySelf: Boolean = false,
    override val targetId: Long? = null,
    override val rawJson: String = "",
) : MessageEvent() {
    override val messageType: String get() = "group"

    //分辨自己发的信息
    override val postType: String get() = if (sentBySelf) "message_sent" else "message"
}
