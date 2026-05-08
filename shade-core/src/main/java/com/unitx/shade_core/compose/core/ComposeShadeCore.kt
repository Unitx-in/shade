package com.unitx.shade_core.compose.core

import com.unitx.shade_core.common.action.ShadeAction
import com.unitx.shade_core.compose.registrar.NoOpRegistrar
import com.unitx.shade_core.compose.handler.ComposeCameraHandler
import com.unitx.shade_core.compose.handler.ComposeDocumentHandler
import com.unitx.shade_core.compose.handler.ComposeGalleryHandler
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.core.ShadeCore

/**
 * [ShadeCore] variant for Compose.
 *
 * Owns nothing except three handlers and routing — mirrors the structure
 * of the XML-side [ShadeCore] which delegates to [CameraHandler],
 * [GalleryHandler], and [DocumentHandler].
 */
internal class ComposeShadeCore(
    config: ShadeConfig,
    private val cameraHandler: ComposeCameraHandler,
    private val galleryHandler: ComposeGalleryHandler,
    private val documentHandler: ComposeDocumentHandler,
) : ShadeCore(registrar = NoOpRegistrar, config = config) {

    override fun registerLaunchers() {
        // Launchers are registered via rememberLauncherForActivityResult in
        // rememberShade — nothing to do here.
    }

    override fun implementHandlers() {
        // Handlers are wired by
        // rememberShade — nothing to do here.
    }

    override fun launch(action: ShadeAction) {
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