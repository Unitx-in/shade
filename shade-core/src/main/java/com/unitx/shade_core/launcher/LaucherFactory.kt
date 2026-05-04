package com.unitx.shade_core.launcher

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.unitx.shade_core.action.ComposeRegistrar

/**
 * Abstraction that lets [ComposeRegistrar] retrieve pre-registered
 * launchers by contract type without knowing about Compose internals.
 */
internal interface LauncherFactory {
    fun <I, O> get(
        contract: ActivityResultContract<I, O>,
        callback: (O) -> Unit
    ): ActivityResultLauncher<I>
}