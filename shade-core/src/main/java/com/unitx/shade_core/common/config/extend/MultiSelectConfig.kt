package com.unitx.shade_core.common.config.extend

/**
 * Configuration for multi-item selection in gallery or document pickers.
 *
 * ```kotlin
 * multiSelect {
 *     enabled = true
 *     maxItems = 5
 * }
 * ```
 *
 * Note: `maxItems` is enforced for image/video gallery but not for
 * document picking due to Android system picker limitations.
 */
data class MultiSelectConfig(

    /** Whether multi-select is active. Default: `false`. */
    var enabled: Boolean = false,

    /** Maximum number of selectable items. Default: unlimited. */
    var maxItems: Int = Int.MAX_VALUE
)
