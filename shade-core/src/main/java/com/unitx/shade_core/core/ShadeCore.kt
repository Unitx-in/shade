package com.unitx.shade_core.core

import com.unitx.shade_core.common.action.ShadeAction
import com.unitx.shade_core.common.config.ShadeConfig
import com.unitx.shade_core.handler.CameraHandler
import com.unitx.shade_core.handler.DocumentHandler
import com.unitx.shade_core.handler.GalleryHandler
import com.unitx.shade_core.registrar.ShadeRegistrar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Core engine. Do not instantiate directly — use [Shade.with] (XML / Activity)
 * or [rememberShade] (Compose).
 *
 * Owns a [CoroutineScope] backed by [SupervisorJob] + [Dispatchers.Main].
 * IO-heavy operations (cache copy in [GalleryHandler] and [DocumentHandler])
 * are dispatched to [Dispatchers.IO] via this scope and results are
 * delivered back on [Dispatchers.Main].
 *
 * Call [cancel] when the host is destroyed to clean up any in-flight work.
 */
open class ShadeCore(
    private val registrar: ShadeRegistrar,
    private val config: ShadeConfig
) {

    private val scope: CoroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    private val registry by lazy { LauncherRegistry(registrar, config) }

    private lateinit var cameraHandler: CameraHandler
    private lateinit var galleryHandler: GalleryHandler
    private lateinit var documentHandler: DocumentHandler

    init {
        initComponents()
    }

    private fun attachLifecycleCleanup() {
        registrar.lifecycleCleanup(scope)
    }

    private fun implementHandlers() {
        val context = registrar.context
        cameraHandler = CameraHandler(context, config, registry, scope)
        galleryHandler = GalleryHandler(context, config, registry, scope)
        documentHandler = DocumentHandler(context, config, registry, scope)
    }

    private fun registerLaunchers() {
        registry.registerConfigured()
    }

    /**
     * Dispatch a [ShadeAction]. Call this from click handlers.
     *
     * ```kotlin
     * binding.btnCamera.setOnClickListener { shade.launch(ShadeAction.Image.Camera) }
     * ```
     */
    open fun launch(action: ShadeAction) {
        when (action) {
            is ShadeAction.Image.Camera -> cameraHandler.handleImageCamera()
            is ShadeAction.Image.Gallery -> galleryHandler.handleImageGallery()
            is ShadeAction.Video.Camera -> cameraHandler.handleVideoCamera()
            is ShadeAction.Video.Gallery -> galleryHandler.handleVideoGallery()
            is ShadeAction.Document -> documentHandler.handleDocument(action)
        }
    }

    open fun initComponents() {
        implementHandlers()
        registerLaunchers()
        attachLifecycleCleanup()
    }
}