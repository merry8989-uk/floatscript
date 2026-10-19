package com.floatscript.app.model

/**
 * Saved position and size of the floating window, in raw screen pixels.
 * Persisted so the window reappears where the user left it.
 */
data class OverlayGeometry(
    val x: Int = -1,
    val y: Int = 200,
    val width: Int = -1,
    val height: Int = 600
) {
    val hasPosition: Boolean get() = x >= 0 && width > 0
}
