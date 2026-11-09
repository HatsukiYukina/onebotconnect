//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.client

import java.time.Duration

//ws连接状态
sealed interface OneBotConnectionState {
    data object Disconnected : OneBotConnectionState
    data object Connecting : OneBotConnectionState
    data object Connected : OneBotConnectionState
    data object Closing : OneBotConnectionState

    data class Reconnecting(
        val attempt: Int,
        val delay: Duration,
        val lastError: Throwable? = null,
    ) : OneBotConnectionState
}

interface OneBotConnectionListener {
    fun onStateChanged(state: OneBotConnectionState) {}

    fun onError(error: Throwable) {}
}
