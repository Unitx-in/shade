package com.unitx.shade_core.registrar

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import kotlinx.coroutines.CoroutineScope

/**
 * Abstraction over the launcher-registration surface that exists on both
 * [androidx.fragment.app.Fragment] and [androidx.activity.ComponentActivity].
 *
 * [com.unitx.shade_core.core.ShadeCore] depends only on this interface, keeping it free of any
 * concrete Fragment or Activity import — which lets the Compose module
 * supply its own implementation backed by [rememberLauncherForActivityResult].
 */
interface ShadeRegistrar {

    /** The [Context] used for file I/O, MIME resolution, permissions, etc. */
    val context: Context

    /**
     * Registers an [ActivityResultContract] and returns a launcher.
     * Must be called before the host reaches STARTED state.
     */
    fun <I, O> register(
        contract: ActivityResultContract<I, O>,
        callback: (O) -> Unit
    ): ActivityResultLauncher<I>

    /**
     * Returns true if the system would show a permission rationale UI
     * for [permission] (i.e. the user previously denied but did not
     * check "Don't ask again").
     */
    fun shouldShowRationale(permission: String): Boolean

    fun lifecycleCleanup(scope: CoroutineScope)
}