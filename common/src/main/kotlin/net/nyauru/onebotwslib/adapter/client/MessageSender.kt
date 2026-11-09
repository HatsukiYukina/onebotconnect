//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: 烟花

package net.nyauru.onebotwslib.adapter.client

import net.nyauru.onebotwslib.adapter.message.MessageSegment
import java.util.concurrent.CompletableFuture

interface MessageSender {
    //发群消息message直接消息段数组发送
    fun sendGroupMessage(
        groupId: Long,
        message: List<MessageSegment>,
    ): CompletableFuture<ActionResponse>

    //发群消息message按cq码解析,纯文本会成text段
    fun sendGroupMessage(
        groupId: Long,
        message: String,
    ): CompletableFuture<ActionResponse>

    //发私聊消息message直接消息段数组发送
    fun sendPrivateMessage(
        userId: Long,
        message: List<MessageSegment>,
    ): CompletableFuture<ActionResponse>

    //发私聊消息message按cq码解析，纯文本会成一个text段
    fun sendPrivateMessage(
        userId: Long,
        message: String,
    ): CompletableFuture<ActionResponse>

    //发送zi定义obv11 action，params用gson可序列化的值
    fun callAction(
        action: String,
        params: Map<String, Any?> = emptyMap(),
    ): CompletableFuture<ActionResponse>
}
