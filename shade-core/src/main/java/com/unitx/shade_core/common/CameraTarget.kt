package com.unitx.shade_core.common

/**
 * Specifies the intended media type when launching the camera.
 *
 * This is used to differentiate between image capture and video recording
 * requests, allowing the system to prepare the correct camera mode and
 * handle the resulting output accordingly.
 */
internal enum class CameraTarget {
    IMAGE,
    VIDEO
}
