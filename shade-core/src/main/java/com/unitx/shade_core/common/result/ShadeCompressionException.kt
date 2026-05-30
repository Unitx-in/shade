package com.unitx.shade_core.common.result

internal class ShadeCompressionException(
    cause: Throwable? = null,
    val failedIndices: List<Int> = emptyList()
) : Exception(cause)