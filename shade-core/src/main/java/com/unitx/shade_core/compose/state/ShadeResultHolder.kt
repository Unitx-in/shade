package com.unitx.shade_core.compose.state

import com.unitx.shade_core.common.result.ShadeError
import com.unitx.shade_core.common.result.ShadeResult

/**
 * Holds [ShadeResult]-typed result and failure callbacks.
 *
 * Raw Android types ([Boolean], [Uri?], [List<Uri>]) are converted to
 * [ShadeResult] at the launcher boundary in [rememberShade], so only
 * [ShadeResult] ever flows through here — consistent with the XML side.
 */
internal class ShadeResultHolder {
    var onResult:  ((ShadeResult) -> Unit)? = null
    var onFailure: ((ShadeError)  -> Unit)? = null

    fun invoke(result: ShadeResult?, fallbackError: ShadeError) {
        if (result != null) onResult?.invoke(result)
        else onFailure?.invoke(fallbackError)
    }
}