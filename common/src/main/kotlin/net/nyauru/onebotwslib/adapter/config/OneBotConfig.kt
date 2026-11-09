//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.config

import java.net.URI
import java.time.Duration


interface OneBotConfig {
    val scheme: String get() = "ws"
    val host: String get() = "127.0.0.1"
    val port: Int get() = 6700
    val path: String get() = "/"
    val token: String? get() = null
    val heartbeatInterval: Duration get() = Duration.ofSeconds(5)
    val autoReconnect: Boolean get() = true
    val reconnectInterval: Duration get() = Duration.ofSeconds(5)
    val actionTimeout: Duration get() = Duration.ofSeconds(30)
    val wsUri: URI
        get() {
            val normalizedPath = if (path.startsWith("/")) path else "/$path"
            return URI(scheme, null, host, port, normalizedPath, null, null)
        }
}
