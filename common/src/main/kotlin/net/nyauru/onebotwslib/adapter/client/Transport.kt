//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.client

import java.net.URI

//连接阶段硬编码超时
internal const val CONNECT_TIMEOUT_MILLIS = 10_000

internal interface Transport {
    fun isOpen(): Boolean
    fun connect()
    fun close(code: Int = 1000, reason: String = "")
    fun send(text: String)
}

internal fun interface TransportFactory {
    fun create(
        uri: URI,
        headers: Map<String, String>,
        heartbeatSeconds: Int,
        listener: TransportListener,
    ): Transport
}


internal interface TransportListener {
    fun onOpen()
    fun onText(text: String)
    fun onClose(code: Int, reason: String?, remote: Boolean)
    fun onError(error: Exception)
}
