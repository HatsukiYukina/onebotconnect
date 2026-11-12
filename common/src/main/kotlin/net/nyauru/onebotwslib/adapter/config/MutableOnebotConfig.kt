package net.nyauru.onebotwslib.adapter.config

import java.net.URI
import java.time.Duration
//提供构造器及工厂方法
class MutableOnebotConfig @JvmOverloads constructor (
    //val? var!
    override var scheme: String = "ws",
    override var host: String = "localhost",
    override var port: Int = 6700,
    override var path: String = "/",
    override var token: String? = null,
    override var heartbeatInterval: Duration = Duration.ofSeconds(5),
    override var autoReconnect: Boolean = true,
    override var reconnectInterval: Duration = Duration.ofSeconds(5),
    override var actionTimeout: Duration = Duration.ofSeconds(30)
) : OneBotConfig{
    override val wsUri: URI
        get() {
            val normalizedPath = if (path.startsWith("/")) path else "/$path"
            return URI(scheme, null, host, port, normalizedPath, null, null)
        }
    init {
        require(scheme == "ws" || scheme == "wss"){"Scheme must be ws"}
        require(port in 1..65535) { "Port must be in 1..65535" }
        require(path.startsWith("/")) { "path must start with /" }
        require(!reconnectInterval.isNegative) { "reconnectInterval must not be negative" }
        require(actionTimeout.isPositive) { "actionTimeout must be positive" }
        require(!heartbeatInterval.isNegative) { "heartbeatInterval must not be negative" }
        require(host.isNotBlank()){"host must not be blank"}
    }
}