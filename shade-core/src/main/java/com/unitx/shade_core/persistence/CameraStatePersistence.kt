package com.unitx.shade_core.persistence

import android.content.Context
import android.net.Uri
import java.io.File
import androidx.core.content.edit
import androidx.core.net.toUri

internal class CameraStatePersistence(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("shade_temp", Context.MODE_PRIVATE)

    var file: File? = null
    var uri: Uri? = null

    fun save(file: File, uri: Uri) {
        this.file = file
        this.uri = uri
        prefs.edit {
            putString("temp_file", file.absolutePath)
                .putString("temp_uri", uri.toString())
        }
    }

    fun restore() {
        if (file != null && uri != null) return
        val path = prefs.getString("temp_file", null)
        val uriStr = prefs.getString("temp_uri", null)
        if (path != null && uriStr != null) {
            file = File(path)
            uri = uriStr.toUri()
        }
    }

    fun clear() {
        file = null
        uri = null
        prefs.edit { clear() }
    }
}