package com.unitx.shade_core.common.config.extend

import com.unitx.shade_core.common.config.base.CameraConfig

class VideoCameraConfig : CameraConfig() {

    /**
     * Maximum duration of the recorded video in seconds.
     * Null means no limit is applied.
     *
     * Note: some devices silently ignore this value.
     */
    var durationLimit: Int? = null

    /**
     * Recording quality hint passed to the camera app.
     * Use `0` for low quality (smaller file) or `1` for high quality (default).
     *
     * Note: support varies by device and camera app.
     */
    var videoQuality: Int? = null

    /**
     * Maximum file size of the recorded video in bytes.
     * The camera app will stop recording once this limit is reached.
     * Null means no limit is applied.
     *
     * Note: some devices silently ignore this value.
     */
    var sizeLimit: Long? = null
}