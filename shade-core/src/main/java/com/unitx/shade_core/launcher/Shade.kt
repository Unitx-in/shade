package com.unitx.shade_core

import androidx.fragment.app.Fragment
import com.unitx.shade_core.registrar.FragmentRegistrar
import com.unitx.shade_core.core.ShadeCore
import com.unitx.shade_core.dsl.ShadeConfig

object Shade {

    fun with(fragment: Fragment, block: ShadeConfig.() -> Unit): ShadeCore {
        val config = ShadeConfig().apply(block)
        val registrar = FragmentRegistrar(fragment)
        return ShadeCore(registrar, config)
    }
}