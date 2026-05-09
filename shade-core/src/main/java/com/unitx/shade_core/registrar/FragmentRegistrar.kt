package com.unitx.shade_core.registrar

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

/**
 * [ShadeRegistrar] implementation for XML / Fragment-based screens.
 *
 * Wrap your [Fragment] with this and pass it to [com.unitx.shade_core.core.ShadeCore] (or use
 * [Shade.with] which does this for you).
 */
internal class FragmentRegistrar(private val fragment: Fragment) : ShadeRegistrar {

    override val context: Context
        get() = fragment.requireContext()

    override fun <I, O> register(
        contract: ActivityResultContract<I, O>,
        callback: (O) -> Unit
    ): ActivityResultLauncher<I> =
        fragment.registerForActivityResult(contract, callback)

    override fun shouldShowRationale(permission: String): Boolean =
        fragment.shouldShowRequestPermissionRationale(permission)

    override fun lifecycleCleanup(scope: CoroutineScope) {
        fragment.viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
            }
        })
    }
}