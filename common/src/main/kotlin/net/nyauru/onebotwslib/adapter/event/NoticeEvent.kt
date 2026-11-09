//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.event

data class NoticeEvent(
    override val time: Long,
    override val selfId: Long,
    val noticeType: String,
    val subType: String? = null,
    val groupId: Long? = null,
    val userId: Long? = null,
    val operatorId: Long? = null,
    val targetId: Long? = null,
    val messageId: Long? = null,
    val duration: Long? = null,
    val file: FileInfo? = null,
    val fields: Map<String, Any?> = emptyMap(),
    override val rawJson: String = "",
) : OneBotEvent() {
    override val postType: String get() = "notice"
}
