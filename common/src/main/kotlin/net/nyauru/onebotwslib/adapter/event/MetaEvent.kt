//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.event


data class MetaEvent(
    override val time: Long,
    override val selfId: Long,
    val metaEventType: String,
    val subType: String? = null,
    val interval: Long? = null,
    val status: BotStatus? = null,
    val fields: Map<String, Any?> = emptyMap(),
    override val rawJson: String = "",
) : OneBotEvent() {
    override val postType: String get() = "meta_event"
}
