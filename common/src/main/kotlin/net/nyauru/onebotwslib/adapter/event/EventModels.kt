//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Assisted-by: Qwen:qwen3.8-flash-next
//该代码使用了llm辅助，按必要的披露责任将使用的llm列出
//This code use LLM assistance and list the LLM used according to necessary disclosure responsibilities.
//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.event

//消息事件发送者信息
data class Sender(
    val userId: Long? = null,
    val nickname: String? = null,
    val card: String? = null,
    val role: String? = null,
    val title: String? = null,
    val sex: String? = null,
    val age: Int? = null,
    val area: String? = null,
    val level: String? = null,
)

//群消息匿名发送者信息
data class Anonymous(
    val id: Long? = null,
    val name: String? = null,
    val flag: String? = null,
)

//文件信息
data class FileInfo(
    val id: String? = null,
    val name: String? = null,
    val size: Long? = null,
    val busid: Int? = null,
    val url: String? = null,
    val file: String? = null,
)

//心跳事件中的机器人状态   extra保存未建模的原始字段
data class BotStatus(
    val online: Boolean? = null,
    val good: Boolean? = null,
    val extra: Map<String, Any?> = emptyMap(),
)
