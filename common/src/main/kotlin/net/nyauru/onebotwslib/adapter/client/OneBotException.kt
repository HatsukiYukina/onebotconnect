//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: 烟花
package net.nyauru.onebotwslib.adapter.client

import java.time.Duration

open class OneBotException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class OneBotConnectionException(
    message: String,
    cause: Throwable? = null,
) : OneBotException(message, cause)

class OneBotProtocolException(
    message: String,
    cause: Throwable? = null,
) : OneBotException(message, cause)

class OneBotActionTimeoutException(
    val action: String,
    val echo: String,
    val timeout: Duration,
) : OneBotException("Action '$action' timed out after $timeout (echo=$echo)")
