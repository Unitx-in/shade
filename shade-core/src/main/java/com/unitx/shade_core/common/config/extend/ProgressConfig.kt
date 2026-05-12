package com.unitx.shade_core.common.config.extend

sealed class ProgressConfig {
    data class Copying(val percent: Int, val fileNumber: Int) : ProgressConfig()
    data class Compressing(val percent: Int, val fileNumber: Int) : ProgressConfig()
}