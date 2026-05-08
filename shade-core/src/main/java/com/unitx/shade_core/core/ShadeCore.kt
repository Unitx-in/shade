package com.unitx.shade_core.core

import com.unitx.shade_core.common.action.ShadeAction
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.handler.CameraHandler
import com.unitx.shade_core.handler.DocumentHandler
import com.unitx.shade_core.handler.GalleryHandler
import com.unitx.shade_core.registrar.ShadeRegistrar

/**
 * Core engine. Do not instantiate directly — use [Shade.with] (XML / Activity)
 * or [rememberShade] (Compose).
 *
 * [ShadeCore] owns nothing except routing. All media logic lives in:
 * - [CameraHandler]  — camera permission, temp file state, image/video capture
 * - [GalleryHandler] — image/video gallery picking, media permission
 * - [DocumentHandler] — PDF and document picking, cache copy
 *
 * Launchers are registered via [LauncherRegistry] which delegates to
 * [ShadeRegistrar], keeping [ShadeCore] free of any Fragment/Activity import.
 */
open class ShadeCore(
    private val registrar: ShadeRegistrar,
    private val config: ShadeConfig
) {

    private val registry = LauncherRegistry(registrar, config)

    private lateinit var cameraHandler: CameraHandler
    private lateinit var galleryHandler : GalleryHandler
    private lateinit var documentHandler : DocumentHandler

    init {
        implementHandlers()
        registerLaunchers()
    }

    open fun implementHandlers(){
        val context = registrar.context
        cameraHandler = CameraHandler(context, config, registry)
        galleryHandler = GalleryHandler(context, config, registry)
        documentHandler = DocumentHandler(context, config, registry)
    }

    open fun registerLaunchers() {
        registry.registerAll()
    }

    /**
     * Dispatch a [ShadeAction]. Call this from click handlers or Compose
     * event callbacks.
     *
     * ```kotlin
     * // XML / Activity
     * binding.btnCamera.setOnClickListener { shade.launch(ShadeAction.Image.Camera) }
     *
     * // Compose
     * Button(onClick = { shade.launch(ShadeAction.Image.Camera) }) { Text("Camera") }
     * ```
     */
    open fun launch(action: ShadeAction) {
        when (action) {
            is ShadeAction.Image.Camera -> cameraHandler.handleImageCamera()
            is ShadeAction.Image.Gallery -> galleryHandler.handleImageGallery()
            is ShadeAction.Video.Camera -> cameraHandler.handleVideoCamera()
            is ShadeAction.Video.Gallery -> galleryHandler.handleVideoGallery()
            is ShadeAction.Pdf -> documentHandler.handlePdf()
            is ShadeAction.Document -> documentHandler.handleDocument(action)
        }
    }
}