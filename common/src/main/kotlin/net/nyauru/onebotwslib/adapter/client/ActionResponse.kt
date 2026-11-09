//SPDX-License-Identifier: MIT
//Copyright (c) 2026 烟花
//Licensed under the MIT License. See LICENSE file in the project root for full text.

//Signed-off-by: 烟花

package net.nyauru.onebotwslib.adapter.client

sealed interface ActionData {
    data class ObjectData(val values: Map<String, Any?>) : ActionData {
        fun get(key: String): Any? = values[key]

        fun string(key: String): String? = values[key]?.toString()

        //number?string!
        fun long(key: String): Long? = when (val value = values[key]) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }

        fun int(key: String): Int? = long(key)?.toInt()
        fun boolean(key: String): Boolean? = when (val value = values[key]) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull()
            else -> null
        }

        fun stringList(key: String): List<String>? =
            (values[key] as? List<*>)?.mapNotNull { it?.toString() }

        val messageId: Long? get() = long("message_id")
    }

    data class ArrayData(val values: List<Any?>) : ActionData
    data class ScalarData(val value: Any?) : ActionData
}

//obaction原始响应
data class ActionResponse(
    val status: String,
    val retcode: Int,
    val data: ActionData?,
    val echo: String?,
    val message: String?,
    val rawJson: String,
) {
    val isOk: Boolean get() = status == "ok"
    val isAsync: Boolean get() = status == "async"
    val isFailed: Boolean get() = status == "failed"
    val isSuccess: Boolean get() = isOk || isAsync
    val objectData: ActionData.ObjectData? get() = data as? ActionData.ObjectData
}
