package com.unitx.shade_core.registrar

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

/**
 * [ShadeRegistrar] implementation for [ComponentActivity]-based hosts
 * (used by the Compose module internally, but also available if you want
 * to drive Shade from an Activity directly in XML projects).
 */
internal class ActivityRegistrar(private val activity: ComponentActivity) : ShadeRegistrar {

    override val context: Context
        get() = activity

    override fun <I, O> register(
        contract: ActivityResultContract<I, O>,
        callback: (O) -> Unit
    ): ActivityResultLauncher<I> =
        activity.registerForActivityResult(contract, callback)

    override fun shouldShowRationale(permission: String): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

    override fun lifecycleCleanup(scope: CoroutineScope) {
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
            }
        })
    }
}