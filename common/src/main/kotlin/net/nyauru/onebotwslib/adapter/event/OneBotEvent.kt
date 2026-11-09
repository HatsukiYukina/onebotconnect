//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.event

sealed class OneBotEvent {
    abstract val time: Long
    abstract val selfId: Long
    abstract val postType: String

    abstract val rawJson: String
}

data class UnknownOneBotEvent(
    override val time: Long,
    override val selfId: Long,
    override val postType: String,
    val fields: Map<String, Any?> = emptyMap(),
    override val rawJson: String = "",
) : OneBotEvent()
