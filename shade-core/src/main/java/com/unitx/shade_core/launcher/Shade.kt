package com.unitx.shade_core.launcher

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import com.unitx.shade_core.core.ShadeCore
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.registrar.ActivityRegistrar
import com.unitx.shade_core.registrar.FragmentRegistrar
import com.unitx.shade_core.interop.JavaUnitCallback

/**
 * Entry point for Activity and Fragment based screens.
 * For Compose, use `rememberShade { }` instead.
 *
 * ## Fragment
 * ```kotlin
 * private val shade by lazy {
 *     Shade.with(fragment = this) {
 *         image {
 *             camera {
 *                 onResult { result -> }
 *                 onFailure { error -> }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Activity
 * ```kotlin
 * // ✅ Must be a class-level property — NOT inside onCreate or by lazy
 * private val shade = Shade.with(activity = this) {
 *     image {
 *         gallery {
 *             onResult { result -> }
 *             onFailure { error -> }
 *         }
 *     }
 * }
 * ```
 *
 * ## FileProvider (required for camera)
 * ```xml
 * <provider
 *     android:name="androidx.core.content.FileProvider"
 *     android:authorities="${applicationId}.fileprovider"
 *     android:exported="false"
 *     android:grantUriPermissions="true">
 *     <meta-data
 *         android:name="android.support.FILE_PROVIDER_PATHS"
 *         android:resource="@xml/shade_file_paths" />
 * </provider>
 * ```
 */
object Shade {

    /**
     * Creates a [ShadeCore] bound to [fragment].
     * Safe to use with `by lazy` at the fragment class level.
     */
    fun with(fragment: Fragment, block: ShadeConfig.() -> Unit): ShadeCore {
        val config = ShadeConfig().apply {
            setFilesProviderAuthority("${fragment.requireContext().packageName}.provider")
            apply(block)
        }
        val registrar = FragmentRegistrar(fragment)
        return ShadeCore(registrar, config)
    }

    /**
     * Java-friendly overload of [with] for [fragment]. Avoids requiring
     * `return null;` from Java lambdas.
     *
     * Creates a [ShadeCore] bound to [fragment].
     * Safe to use with `by lazy` at the fragment class level.
     */
    fun with(fragment: Fragment, block: JavaUnitCallback<ShadeConfig>): ShadeCore =
        with(fragment) { block.invoke(this) }

    /**
     * Creates a [ShadeCore] bound to [activity].
     *
     * Must be initialised as a class-level property before `onCreate` returns.
     * `by lazy` is not safe here.
     */
    fun with(activity: ComponentActivity, block: ShadeConfig.() -> Unit): ShadeCore {
        val config = ShadeConfig().apply {
            setFilesProviderAuthority("${activity.packageName}.provider")
            apply(block)
        }
        val registrar = ActivityRegistrar(activity)
        return ShadeCore(registrar, config)
    }

    /**
     * Java-friendly overload of [with] for [activity]. Avoids requiring
     * `return null;` from Java lambdas.
     *
     * Creates a [ShadeCore] bound to [activity].
     *
     * Must be initialised as a class-level property before `onCreate` returns.
     * `by lazy` is not safe here.
     */
    fun with(activity: ComponentActivity, block: JavaUnitCallback<ShadeConfig>): ShadeCore =
        with(activity) { block.invoke(this) }
}