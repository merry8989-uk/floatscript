package com.floatscript.app.model

/**
 * Saved position and size of the floating window, expressed as **fractions of
 * the screen** rather than raw pixels, so the window keeps the same relative
 * place after a rotation or on a different screen size.
 */
data class OverlayGeometry(
    /** < 0 means "no saved position yet" — the service picks a default. */
    val xFraction: Float = -1f,
    val yFraction: Float = 0.18f,
    val widthFraction: Float = 1f,
    val heightFraction: Float = 0.40f
) {
    val hasPosition: Boolean get() = xFraction >= 0f

    companion object {
        /** Clamp helper used by the service when converting to pixels. */
        fun fraction(value: Int, total: Int): Float =
            if (total <= 0) 0f else value.toFloat() / total.toFloat()
    }
}
