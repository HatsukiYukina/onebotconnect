//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Assisted-by: Qwen:qwen3.8-flash-next
//该代码使用了llm辅助，按必要的披露责任将使用的llm列出
//This code use LLM assistance and list the LLM used according to necessary disclosure responsibilities.
//Signed-off-by: 烟花

package net.nyauru.onebotwslib.adapter.client

import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.enums.ReadyState
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

internal class JavaWebSocketTransport(
    uri: URI,
    headers: Map<String, String>,
    heartbeatSeconds: Int,
    private val callbacks: TransportListener,
) : WebSocketClient(uri, Draft_6455(), headers, CONNECT_TIMEOUT_MILLIS), Transport {

    init {
        //守护线程
        setDaemon(true)
        setTcpNoDelay(true)
        //心跳
        setConnectionLostTimeout(heartbeatSeconds)
    }

    override fun connect() {
        super.connect()
    }

    override fun close(code: Int, reason: String) {
        super.close(code, reason)
        if (readyState == ReadyState.NOT_YET_CONNECTED) {
            try {
                getSocket()?.close()
            } catch (_: Exception) {
            }
        }
    }

    override fun send(text: String) {
        super.send(text)
    }

    override fun onOpen(handshakedata: ServerHandshake) {
        callbacks.onOpen()
    }

    override fun onMessage(message: String) {
        callbacks.onText(message)
    }

    override fun onMessage(bytes: ByteBuffer) {
        //我们这不兴二进制祷言
        callbacks.onError(OneBotProtocolException("not supported binary WS frames"))
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        callbacks.onClose(code, reason, remote)
    }

    override fun onError(ex: Exception) {
        callbacks.onError(ex)
    }
}

internal object JavaWebSocketTransportFactory : TransportFactory {
    override fun create(
        uri: URI,
        headers: Map<String, String>,
        heartbeatSeconds: Int,
        listener: TransportListener,
    ): Transport = JavaWebSocketTransport(uri, headers, heartbeatSeconds, listener)
}
