package com.unitx.shade_core.launcher

import androidx.fragment.app.Fragment
import com.unitx.shade_core.core.ShadeCore
import com.unitx.shade_core.config.ShadeConfig
import com.unitx.shade_core.registrar.FragmentRegistrar

/**
 * Entry point for **XML / Fragment** based screens.
 *
 * For Compose, use `rememberShade { }` from the `shade-compose` module instead.
 *
 * ## Usage
 *
 * ```kotlin
 * class UploadFragment : Fragment() {
 *
 *     // Initialise lazily — launchers register before STARTED state
 *     private val shade by lazy {
 *         Shade.with(fragment = this) {
 *
 *             image {
 *                 camera {
 *                     onResult { result ->
 *                         // result: ShadeResult.Captured
 *                         viewModel.onImageCaptured(result.file, result.uri)
 *                     }
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
 *
 *             video {
 *                 camera {
 *                     onResult { result -> viewModel.onVideoRecorded(result.file, result.uri) }
 *                     onFailure { error -> showError(error) }
 *                 }
 *                 gallery {
 *                     onResult { result -> viewModel.onVideoPicked((result as ShadeResult.Single).uri) }
 *                     onFailure { error -> showError(error) }
 *                 }
 *             }
 *
 *             pdf {
 *                 onResult { result -> viewModel.onPdfPicked(result.uri, result.file!!) }
 *                 onFailure { error -> showError(error) }
 *             }
 *
 *             document {
 *                 copyToCache = true
 *                 onResult { result -> viewModel.onDocumentPicked(result.uri, result.file) }
 *                 onFailure { error -> showError(error) }
 *             }
 *         }
 *     }
 *
 *     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *         super.onViewCreated(view, savedInstanceState)
 *         binding.btnCamera.setOnClickListener { shade.launch(ShadeAction.Image.Camera) }
 *         binding.btnGallery.setOnClickListener { shade.launch(ShadeAction.Image.Gallery) }
 *         binding.btnRecord.setOnClickListener  { shade.launch(ShadeAction.Video.Camera)  }
 *         binding.btnPdf.setOnClickListener     { shade.launch(ShadeAction.Pdf)           }
 *         binding.btnDoc.setOnClickListener     {
 *             shade.launch(ShadeAction.Document()) // default MIME types
 *         }
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
     * Must be called before the fragment reaches STARTED state so that
     * activity result launchers register correctly. A `by lazy` property
     * at the fragment class level is the recommended pattern.
     *
     * @param fragment The fragment that hosts the media flows.
     * @param block    DSL configuration block.
     */
    fun with(fragment: Fragment, block: ShadeConfig.() -> Unit): ShadeCore {
        val config = ShadeConfig().apply(block)
        val registrar = FragmentRegistrar(fragment)
        return ShadeCore(registrar, config)
    }
}