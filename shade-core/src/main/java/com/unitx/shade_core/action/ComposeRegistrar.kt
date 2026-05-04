package com.unitx.shade_core.action

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.unitx.shade_core.core.ShadeRegistrar
import com.unitx.shade_core.launcher.LauncherFactory

internal class ComposeRegistrar(
    override val context: Context,
    private val launcherFactory: LauncherFactory,
    private val rationaleChecker: (String) -> Boolean
) : ShadeRegistrar {

    override fun <I, O> register(
        contract: ActivityResultContract<I, O>,
        callback: (O) -> Unit
    ): ActivityResultLauncher<I> = launcherFactory.get(contract, callback)

    override fun shouldShowRationale(permission: String): Boolean =
        rationaleChecker(permission)
}