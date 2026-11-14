//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Assisted-by: Qwen:qwen3.8-flash-next Deepseek:deepseekv4.1-flash-1017
//该代码使用了llm辅助，按必要的披露责任将使用的llm列出
//This code use LLM assistance and list the LLM used according to necessary disclosure responsibilities.
//Signed-off-by: 烟花

package net.nyauru.onebotwslib.adapter.client

import net.nyauru.onebotwslib.adapter.config.DefaultOneBotConfig
import net.nyauru.onebotwslib.adapter.config.OneBotConfig
import net.nyauru.onebotwslib.adapter.event.MetaEvent
import net.nyauru.onebotwslib.adapter.event.MessageEvent
import net.nyauru.onebotwslib.adapter.event.MessageEventListener
import net.nyauru.onebotwslib.adapter.event.NoticeEvent
import net.nyauru.onebotwslib.adapter.event.OneBotEvent
import net.nyauru.onebotwslib.adapter.event.RequestEvent
import net.nyauru.onebotwslib.adapter.message.MessageSegment
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong


class OneBotClient internal constructor(
    val config: OneBotConfig,
    private val listener: MessageEventListener,
    callbackExecutor: Executor?,
    private val transportFactory: TransportFactory,
) : MessageSender, AutoCloseable {

    constructor(
        config: OneBotConfig = DefaultOneBotConfig(),
        listener: MessageEventListener = MessageEventListener.EMPTY,
        callbackExecutor: Executor? = null,
    ) : this(config, listener, callbackExecutor, JavaWebSocketTransportFactory)

    private val ownsExecutor = callbackExecutor == null

    private val executor: Executor =
        callbackExecutor ?: Executors.newSingleThreadExecutor(daemonThreadFactory("OneBotClient-events"))

    private val scheduler = ScheduledThreadPoolExecutor(1, daemonThreadFactory("OneBotClient-scheduler")).apply {
        removeOnCancelPolicy = true
    }

    //？！锁锁！？
    private val lifecycleLock = Any()
    private val connectionListeners = CopyOnWriteArrayList<OneBotConnectionListener>()

    private val pending = ConcurrentHashMap<String, PendingAction>()
    private val nextEcho = AtomicLong(0)

    @Volatile
    private var currentTransport: Transport? = null

    //连接状态变化异步通知OneBotConnectionListener
    @Volatile
    var state: OneBotConnectionState = OneBotConnectionState.Disconnected
        private set

    @Volatile
    private var reconnectTask: ScheduledFuture<*>? = null

    //连接阶段看门狗
    private var connectWatchdogTask: ScheduledFuture<*>? = null
    private var reconnectAttempt = 0
    private var userClosed = false
    private var terminalClosed = false

    val isConnected: Boolean
        get() = state == OneBotConnectionState.Connected && currentTransport?.isOpen() == true

    fun addConnectionListener(listener: OneBotConnectionListener) {
        connectionListeners.addIfAbsent(listener)
    }

    fun removeConnectionListener(listener: OneBotConnectionListener) {
        connectionListeners.remove(listener)
    }

    fun connect() {
        synchronized(lifecycleLock) {
            check(!terminalClosed) { "OneBotClient is closed" }
            if (currentTransport != null) return
            reconnectTask?.cancel(false)
            reconnectTask = null
            cancelConnectWatchdogLocked()
            userClosed = false
            reconnectAttempt = 0
            startTransportLocked()
        }
    }

    //超时返回false
    fun connectBlocking(timeout: Duration = Duration.ofSeconds(10)): Boolean {
        val latch = CountDownLatch(1)
        val result = AtomicBoolean(false)
        val connectionListener = object : OneBotConnectionListener {
            override fun onStateChanged(state: OneBotConnectionState) {
                when (state) {
                    OneBotConnectionState.Connected -> {
                        result.set(true)
                        latch.countDown()
                    }

                    OneBotConnectionState.Disconnected -> {
                        result.set(false)
                        latch.countDown()
                    }

                    else -> Unit
                }
            }
        }

        addConnectionListener(connectionListener)
        return try {
            connect()
            latch.await(timeout.toMillis().coerceAtLeast(0), TimeUnit.MILLISECONDS)
            result.get()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        } finally {
            removeConnectionListener(connectionListener)
        }
    }

    fun disconnect() {
        val transport: Transport?
        synchronized(lifecycleLock) {
            if (terminalClosed) return
            userClosed = true
            reconnectTask?.cancel(false)
            reconnectTask = null
            cancelConnectWatchdogLocked()
            reconnectAttempt = 0
            transport = currentTransport
            currentTransport = null
            setState(OneBotConnectionState.Closing)
        }
        if (transport != null) {
            try {
                transport.close()
            } catch (t: Throwable) {
                reportError(t)
            }
        }
        failPendingActions("client disconnected")
        setState(OneBotConnectionState.Disconnected)
    }

    override fun close() {
        val transport: Transport?
        synchronized(lifecycleLock) {
            if (terminalClosed) return
            terminalClosed = true
            userClosed = true
            reconnectTask?.cancel(false)
            reconnectTask = null
            cancelConnectWatchdogLocked()
            transport = currentTransport
            currentTransport = null
            setState(OneBotConnectionState.Closing)
        }
        if (transport != null) {
            try {
                transport.close()
            } catch (_: Throwable) {
            }
        }
        failPendingActions("client closed")
        setState(OneBotConnectionState.Disconnected)
        scheduler.shutdownNow()
        if (ownsExecutor) (executor as? ExecutorService)?.shutdownNow()
    }

    override fun sendGroupMessage(
        groupId: Long,
        message: List<MessageSegment>,
    ): CompletableFuture<ActionResponse> = callAction(
        "send_group_msg",
        mapOf(
            "group_id" to groupId,
            "message" to OneBotJson.messageArray(message),
        ),
    )

    override fun sendGroupMessage(
        groupId: Long,
        message: String,
    ): CompletableFuture<ActionResponse> = sendGroupMessage(groupId, MessageSegment.parseCQ(message))

    override fun sendPrivateMessage(
        userId: Long,
        message: List<MessageSegment>,
    ): CompletableFuture<ActionResponse> = callAction(
        "send_private_msg",
        mapOf(
            "user_id" to userId,
            "message" to OneBotJson.messageArray(message),
        ),
    )

    override fun sendPrivateMessage(
        userId: Long,
        message: String,
    ): CompletableFuture<ActionResponse> = sendPrivateMessage(userId, MessageSegment.parseCQ(message))


    override fun callAction(
        action: String,
        params: Map<String, Any?>,
    ): CompletableFuture<ActionResponse> {
        val transport = currentTransport
        if (transport == null || !transport.isOpen()) {
            return CompletableFuture.failedFuture(OneBotConnectionException("not connected"))
        }

        val echo = "obc-${nextEcho.incrementAndGet()}"
        val future = CompletableFuture<ActionResponse>()
        val pendingAction = PendingAction(future)
        pending[echo] = pendingAction
        future.whenComplete { _, _ ->
            pending.remove(echo)?.timeoutTask?.cancel(false)
        }

        pendingAction.timeoutTask = try {
            scheduler.schedule(
                {
                    val removed = pending.remove(echo)
                    if (removed != null) {
                        removed.future.completeExceptionally(
                            OneBotActionTimeoutException(action, echo, config.actionTimeout),
                        )
                    }
                },
                config.actionTimeout.toMillis().coerceAtLeast(1),
                TimeUnit.MILLISECONDS,
            )
        } catch (e: RejectedExecutionException) {
            pending.remove(echo)
            future.completeExceptionally(OneBotConnectionException("client is closed", e))
            return future
        }

        try {
            transport.send(OneBotJson.actionFrame(action, params, echo))
        } catch (t: Throwable) {
            pending.remove(echo)
            pendingAction.timeoutTask?.cancel(false)
            future.completeExceptionally(OneBotConnectionException("failed to send action '$action'", t))
        }
        return future
    }

    private fun startTransportLocked() {
        if (terminalClosed || userClosed || currentTransport != null) return
        setState(OneBotConnectionState.Connecting)

        lateinit var transport: Transport
        transport = transportFactory.create(
            config.wsUri,
            authHeaders(),
            heartbeatSeconds(),
            object : TransportListener {
                override fun onOpen() {
                    handleOpen(transport)
                }

                override fun onText(text: String) {
                    handleText(transport, text)
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    handleClose(transport, code, reason)
                }

                override fun onError(error: Exception) {
                    handleError(transport, error)
                }
            },
        )
        currentTransport = transport

        try {
            transport.connect()
            scheduleConnectWatchdogLocked(transport)
        } catch (t: Throwable) {
            currentTransport = null
            reportError(t)
            scheduleReconnectOrDisconnectLocked(t)
        }
    }

    private fun handleOpen(transport: Transport) {
        synchronized(lifecycleLock) {
            if (currentTransport !== transport) return
            cancelConnectWatchdogLocked()
            reconnectAttempt = 0
            setState(OneBotConnectionState.Connected)
        }
    }

    private fun handleText(transport: Transport, raw: String) {
        if (currentTransport !== transport) return
        when (val frame = OneBotJson.parseIncoming(raw)) {
            is OneBotJson.IncomingFrame.EventFrame -> dispatchEvent(frame.event)
            is OneBotJson.IncomingFrame.ResponseFrame -> completeAction(frame.response)
            is OneBotJson.IncomingFrame.Invalid -> reportError(frame.error)
        }
    }

    private fun handleClose(transport: Transport, code: Int, reason: String?) {
        synchronized(lifecycleLock) {
            if (currentTransport !== transport) return
            currentTransport = null
            cancelConnectWatchdogLocked()
            val detail = buildString {
                append("WebSocket closed (code=")
                append(code)
                if (!reason.isNullOrBlank()) {
                    append(", reason=")
                    append(reason)
                }
                append(')')
            }
            failPendingActions(detail)
            scheduleReconnectOrDisconnectLocked(null)
        }
    }

    private fun handleError(transport: Transport, error: Exception) {
        reportError(error)
        synchronized(lifecycleLock) {
            if (currentTransport !== transport) return
            if (!transport.isOpen()) {
                currentTransport = null
                cancelConnectWatchdogLocked()
                failPendingActions("WebSocket error: ${error.message}")
                scheduleReconnectOrDisconnectLocked(error)
            }
        }
    }

    private fun scheduleReconnectOrDisconnectLocked(lastError: Throwable?) {
        if (terminalClosed || userClosed || !config.autoReconnect || reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
            setState(OneBotConnectionState.Disconnected)
            return
        }

        reconnectAttempt += 1
        val delay = nextReconnectDelay()
        setState(OneBotConnectionState.Reconnecting(reconnectAttempt, delay, lastError))
        reconnectTask = try {
            scheduler.schedule(
                {
                    synchronized(lifecycleLock) {
                        if (terminalClosed || userClosed || currentTransport != null) return@synchronized
                        startTransportLocked()
                    }
                },
                delay.toMillis().coerceAtLeast(0),
                TimeUnit.MILLISECONDS,
            )
        } catch (_: RejectedExecutionException) {
            null
        }
    }

    //重连延迟指数退避
    private fun nextReconnectDelay(): Duration {
        val maxMillis = MAX_RECONNECT_INTERVAL.toMillis()
        val baseMillis = config.reconnectInterval.toMillis().coerceIn(0L, maxMillis)
        if (baseMillis == 0L) return Duration.ZERO
        val shift = (reconnectAttempt - 1).coerceIn(0, 30)
        return Duration.ofMillis((baseMillis shl shift).coerceAtMost(maxMillis))
    }

    //连接阶段看门狗
    private fun scheduleConnectWatchdogLocked(transport: Transport) {
        cancelConnectWatchdogLocked()
        connectWatchdogTask = try {
            scheduler.schedule(
                {
                    synchronized(lifecycleLock) {
                        if (currentTransport !== transport || transport.isOpen()) return@synchronized
                        currentTransport = null
                        connectWatchdogTask = null
                        try {
                            transport.close()
                        } catch (t: Throwable) {
                            reportError(t)
                        }
                        failPendingActions("connection timed out")
                        scheduleReconnectOrDisconnectLocked(
                            OneBotConnectionException("connection timed out after ${CONNECT_TIMEOUT_MILLIS}ms"),
                        )
                    }
                },
                CONNECT_TIMEOUT_MILLIS.toLong(),
                TimeUnit.MILLISECONDS,
            )
        } catch (_: RejectedExecutionException) {
            null
        }
    }

    private fun cancelConnectWatchdogLocked() {
        connectWatchdogTask?.cancel(false)
        connectWatchdogTask = null
    }

    private fun completeAction(response: ActionResponse) {
        val echo = response.echo
        if (echo.isNullOrBlank()) {
            reportError(OneBotProtocolException("action response without echo"))
            return
        }
        val pendingAction = pending.remove(echo) ?: return
        pendingAction.timeoutTask?.cancel(false)
        pendingAction.future.complete(response)
    }

    private fun failPendingActions(reason: String) {
        for (echo in pending.keys.toList()) {
            val pendingAction = pending.remove(echo) ?: continue
            pendingAction.timeoutTask?.cancel(false)
            pendingAction.future.completeExceptionally(OneBotConnectionException(reason))
        }
    }

    private fun dispatchEvent(event: OneBotEvent) {
        executeSafely {
            invokeListenerSafely { listener.onEvent(event) }
            when (event) {
                is MessageEvent -> invokeListenerSafely { listener.onMessage(event) }
                is NoticeEvent -> invokeListenerSafely { listener.onNotice(event) }
                is RequestEvent -> invokeListenerSafely { listener.onRequest(event) }
                is MetaEvent -> invokeListenerSafely { listener.onMetaEvent(event) }
                else -> Unit
            }
        }
    }

    private fun invokeListenerSafely(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            reportError(t)
        }
    }

    private fun setState(newState: OneBotConnectionState) {
        state = newState
        executeSafely {
            for (connectionListener in connectionListeners) {
                try {
                    connectionListener.onStateChanged(newState)
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun reportError(error: Throwable) {
        executeSafely {
            for (connectionListener in connectionListeners) {
                try {
                    connectionListener.onError(error)
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun executeSafely(block: () -> Unit) {
        try {
            executor.execute {
                try {
                    block()
                } catch (_: Throwable) {
                }
            }
        } catch (_: RejectedExecutionException) {
        }
    }

    private fun authHeaders(): Map<String, String> {
        val token = config.token?.takeIf { it.isNotBlank() } ?: return emptyMap()
        return mapOf("Authorization" to "Bearer $token")
    }

    private fun heartbeatSeconds(): Int =
        config.heartbeatInterval.seconds.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()

    private class PendingAction(
        val future: CompletableFuture<ActionResponse>,
    ) {
        @Volatile
        var timeoutTask: ScheduledFuture<*>? = null
    }

    private companion object {
        //重连间隔上限
        val MAX_RECONNECT_INTERVAL: Duration = Duration.ofSeconds(120)

        //单次断线最多重连上限
        const val MAX_RECONNECT_ATTEMPTS = 10
    }
}

private fun daemonThreadFactory(prefix: String): ThreadFactory {
    val counter = AtomicLong(0)
    return ThreadFactory { runnable ->
        Thread(runnable, "$prefix-${counter.incrementAndGet()}").apply { isDaemon = true }
    }
}
