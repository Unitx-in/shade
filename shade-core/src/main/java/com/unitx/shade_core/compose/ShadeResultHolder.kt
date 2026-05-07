package com.unitx.shade_core.compose

import com.unitx.shade_core.result.ShadeError
import com.unitx.shade_core.result.ShadeResult

internal class ShadeResultHolder {
    var onResult: ((ShadeResult) -> Unit)? = null
    var onFailure: ((ShadeError) -> Unit)? = null

    fun invoke(result: ShadeResult?, fallbackError: ShadeError) {
        if (result != null) onResult?.invoke(result)
        else onFailure?.invoke(fallbackError)
    }
}