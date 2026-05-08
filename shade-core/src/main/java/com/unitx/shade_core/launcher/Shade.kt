package com.unitx.shade_core.launcher

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import com.unitx.shade_core.core.ShadeCore
import com.unitx.shade_core.config.ShadeConfig
import com.unitx.shade_core.registrar.ActivityRegistrar
import com.unitx.shade_core.registrar.FragmentRegistrar

/**
 * Entry point for **XML / Fragment and Activity** based screens.
 *
 * For Compose, use `rememberShade { }` from the `shade-compose` module instead.
 *
 * ## Fragment usage
 *
 * ```kotlin
 * class UploadFragment : Fragment() {
 *
 *     private val shade by lazy {
 *         Shade.with(fragment = this) {
 *             image {
 *                 camera {
 *                     onResult { result -> viewModel.onImageCaptured(result.file, result.uri) }
 *                     onFailure { error -> showError(error) }
 *                 }
 *                 gallery {
 *                     multiSelect(maxItems = 5)
 *                     onResult { result ->
 *                         when (result) {
 *                             is ShadeResult.Single   -> viewModel.addImage(result.uri)
 *                             is ShadeResult.Multiple -> viewModel.addImages(result.uris)
 *                             else -> Unit
 *                         }
 *                     }
 *                     onFailure { error -> showError(error) }
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Activity usage
 *
 * ```kotlin
 * class UploadActivity : ComponentActivity() {
 *
 *     // Must be initialised before onCreate() returns — do NOT use lazy here.
 *     // Launcher registration must happen before the activity reaches STARTED state.
 *     private val shade = Shade.with(activity = this) {
 *         image {
 *             camera {
 *                 onResult { result -> viewModel.onImageCaptured(result.file, result.uri) }
 *                 onFailure { error -> showError(error) }
 *             }
 *         }
 *         pdf {
 *             onResult { result -> viewModel.onPdfPicked(result.uri, result.file!!) }
 *             onFailure { error -> showError(error) }
 *         }
 *     }
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         binding.btnCamera.setOnClickListener { shade.launch(ShadeAction.Image.Camera) }
 *         binding.btnPdf.setOnClickListener    { shade.launch(ShadeAction.Pdf) }
 *     }
 * }
 * ```
 *
 * ## Manifest — FileProvider (required for camera capture)
 *
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
     * Creates a [ShadeCore] instance bound to [fragment].
     *
     * Must be called before the fragment reaches STARTED state.
     * A `by lazy` property at the fragment class level is the recommended pattern.
     *
     * @param fragment The fragment that hosts the media flows.
     * @param block    DSL configuration block.
     */
    fun with(fragment: Fragment, block: ShadeConfig.() -> Unit): ShadeCore {
        val config = ShadeConfig().apply(block)
        val registrar = FragmentRegistrar(fragment)
        return ShadeCore(registrar, config)
    }

    /**
     * Creates a [ShadeCore] instance bound to [activity].
     *
     * **Must be initialised as a class-level property, not inside `onCreate`.**
     * Launcher registration must happen before the activity reaches STARTED state,
     * so `by lazy` is not safe here — the property must be eagerly initialised:
     *
     * ```kotlin
     * // ✅ correct — initialised at property declaration time
     * private val shade = Shade.with(activity = this) { ... }
     *
     * // ❌ wrong — lazy defers until first access, which may be too late
     * private val shade by lazy { Shade.with(activity = this) { ... } }
     * ```
     *
     * @param activity The [ComponentActivity] that hosts the media flows.
     * @param block    DSL configuration block.
     */
    fun with(activity: ComponentActivity, block: ShadeConfig.() -> Unit): ShadeCore {
        val config = ShadeConfig().apply(block)
        val registrar = ActivityRegistrar(activity)
        return ShadeCore(registrar, config)
    }
}