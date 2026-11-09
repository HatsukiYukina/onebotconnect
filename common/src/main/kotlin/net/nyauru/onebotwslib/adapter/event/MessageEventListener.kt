//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.event

//ob事件回调
interface MessageEventListener {
    fun onEvent(event: OneBotEvent) {}

    fun onMessage(event: MessageEvent) {}

    fun onNotice(event: NoticeEvent) {}

    fun onRequest(event: RequestEvent) {}

    fun onMetaEvent(event: MetaEvent) {}

    companion object {
        @JvmField
        val EMPTY: MessageEventListener = object : MessageEventListener {}
    }
}
