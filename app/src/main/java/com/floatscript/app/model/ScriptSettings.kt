package com.floatscript.app.model

import androidx.compose.ui.graphics.Color

/** Reading modes supported by the scrolling engine. */
enum class ScrollMode {
    /** Continuous pixel-by-pixel scroll (default). */
    CONTINUOUS,

    /** Advances one line at a time with a short pause between lines. */
    LINE_BY_LINE,

    /** Highlights and advances a single word at a time. */
    WORD_BY_WORD
}

/** Horizontal / vertical alignment of the script text. */
enum class TextAlign { START, CENTER, END }

/**
 * Every visual and behavioural knob of the teleprompter. Persisted as a whole
 * via DataStore (see [com.floatscript.app.data.SettingsRepository]) so the last
 * used configuration is restored on the next launch.
 */
data class ScriptSettings(
    val textColor: Color = Color.White,
    val fontSizeSp: Float = 28f,
    val bold: Boolean = true,
    val lineHeightMultiplier: Float = 1.25f,
    val align: TextAlign = TextAlign.START,
    /** 0f = fully transparent background, 1f = fully opaque. */
    val backgroundOpacity: Float = 0.35f,
    val backgroundColor: Color = Color.Black,
    /** Horizontal padding inside the floating window, in dp. */
    val horizontalMarginDp: Int = 16,
    /** Scroll speed in pixels per second at the base density. */
    val scrollSpeedPxPerSec: Float = 60f,
    val scrollMode: ScrollMode = ScrollMode.CONTINUOUS,
    val countdownSeconds: Int = 3,
    val autoHideControls: Boolean = true,
    val mirrorHorizontally: Boolean = false,
    val flipVertically: Boolean = false,
    val keepScreenAwake: Boolean = true,
    val speechFollowEnabled: Boolean = false,
    val minimalMode: Boolean = false
) {
    companion object {
        const val MIN_FONT_SP = 12f
        const val MAX_FONT_SP = 96f
        const val MIN_SPEED = 10f
        const val MAX_SPEED = 400f
    }
}
