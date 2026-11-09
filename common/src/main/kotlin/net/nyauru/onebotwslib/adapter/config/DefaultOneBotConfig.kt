//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Assisted-by: Qwen:qwen3.8-flash-next
//该代码使用了llm辅助，按必要的披露责任将使用的llm列出
//This code use LLM assistance and list the LLM used according to necessary disclosure responsibilities.
//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.config

import java.time.Duration

//OneBotConfig的默认实现，接入程序需要自己实现这个
data class DefaultOneBotConfig(
    override val scheme: String = "ws",
    override val host: String = "127.0.0.1",
    override val port: Int = 6700,
    override val path: String = "/",
    override val token: String? = null,
    override val heartbeatInterval: Duration = Duration.ofSeconds(5),
    override val autoReconnect: Boolean = true,
    override val reconnectInterval: Duration = Duration.ofSeconds(5),
    override val actionTimeout: Duration = Duration.ofSeconds(30),
) : OneBotConfig {
    init {
        require(scheme == "ws" || scheme == "wss") { "scheme must be ws" }
        require(host.isNotBlank()) { "host must not be blank" }
        require(port in 1..65535) { "port must be in 1-65535" }
        require(path.startsWith("/")) { "path must start with /" }
        require(!heartbeatInterval.isNegative) { "heartbeatInterval must not be negative" }
        require(!reconnectInterval.isNegative) { "reconnectInterval must not be negative" }
        require(actionTimeout.isPositive) { "actionTimeout must be positive" }
    }
}
