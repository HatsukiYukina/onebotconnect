//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.event

data class RequestEvent(
    override val time: Long,
    override val selfId: Long,
    val requestType: String,
    val subType: String? = null,
    val flag: String? = null,
    val comment: String? = null,
    val groupId: Long? = null,
    val userId: Long? = null,
    val fields: Map<String, Any?> = emptyMap(),
    override val rawJson: String = "",
) : OneBotEvent() {
    override val postType: String get() = "request"
}
