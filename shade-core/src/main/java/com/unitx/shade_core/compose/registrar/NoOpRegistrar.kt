package com.unitx.shade_core.compose.registrar

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.unitx.shade_core.registrar.ShadeRegistrar
import kotlinx.coroutines.CoroutineScope

/** No-op [com.unitx.shade_core.registrar.ShadeRegistrar] stub — only used so [com.unitx.shade_core.core.ShadeCore]'s constructor is satisfied. */
internal object NoOpRegistrar : ShadeRegistrar {
    override val context: Context
        get() = error("NoOpRegistrar.context must never be called")

    override fun <I, O> register(
        contract: ActivityResultContract<I, O>,
        callback: (O) -> Unit
    ): ActivityResultLauncher<I> = error("NoOpRegistrar.register must never be called")

    override fun shouldShowRationale(permission: String) = false
    override fun lifecycleCleanup(scope: CoroutineScope) {
        error("NoOpRegistrar.scopeClear must never be called")
    }
}