package com.floatscript.app.overlay

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.text.TextLayoutResult
import com.floatscript.app.model.ScrollMode
import com.floatscript.app.model.ScriptSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Drives the [ScrollState] according to the active [ScrollMode]. Runs inside a
 * `LaunchedEffect` in [TeleprompterView]; it is cancelled automatically when the
 * composition leaves or when any of its keys change.
 *
 * Frame-based continuous scrolling is tied to `withFrameNanos`, so it stays
 * smooth when the display refresh rate changes (e.g. 60 Hz vs 120 Hz).
 *
 * Word- and line-based modes use the live [TextLayoutResult] so the engine can
 * target the exact vertical position of each word / line, and reports the
 * current word range back to the UI for highlighting.
 */
class ScrollEngine(
    private val scrollState: ScrollState,
    private val settings: ScriptSettings,
    private val layout: TextLayoutResult?,
    private val onWordRange: (IntRange?) -> Unit
) {

    suspend fun run() {
        try {
            when (settings.scrollMode) {
                ScrollMode.CONTINUOUS -> runContinuous()
                ScrollMode.LINE_BY_LINE -> runByLine()
                ScrollMode.WORD_BY_WORD -> runByWord()
            }
        } finally {
            onWordRange(null)
        }
    }

    /** Smooth pixel-by-pixel scrolling, frame-clock driven. */
    private suspend fun runContinuous() {
        var last = 0L
        while (coroutineContext.isActive) {
            val now = withFrameNanos { it }
            if (last != 0L) {
                val dtSeconds = (now - last) / 1_000_000_000.0
                val remaining = (scrollState.maxValue - scrollState.value).toFloat()
                if (remaining <= 0f) return
                val delta = (settings.scrollSpeedPxPerSec * dtSeconds).toFloat()
                scrollState.scrollBy(delta.coerceAtMost(remaining))
            }
            last = now
        }
    }

    /** Advances one rendered line at a time, pausing between lines. */
    private suspend fun runByLine() {
        val lr = layout ?: return
        val intervalMs = (2200f - settings.scrollSpeedPxPerSec * 4f).coerceIn(150f, 2200f)
        for (i in 0 until lr.lineCount) {
            if (!coroutineContext.isActive) return
            val target = lr.getLineTop(i).coerceIn(0f, scrollState.maxValue.toFloat())
            scrollState.animateScrollTo(target.toInt())
            delay(intervalMs.toLong())
        }
    }

    /** Highlights and advances a single word at a time. */
    private suspend fun runByWord() {
        val lr = layout ?: return
        val text = lr.layoutInput.text.text
        val words = TextTokenizer.words(text)
        val intervalMs = (900f - settings.scrollSpeedPxPerSec * 1.6f).coerceIn(80f, 900f)
        val anchor = 0.30f // keep the active word ~30% down the viewport
        for (span in words) {
            if (!coroutineContext.isActive) return
            val box = runCatching { lr.getBoundingBox(span.start) }.getOrNull()
            val viewport = scrollState.viewportSize
            val target = if (box != null && viewport > 0) {
                (box.top - viewport * anchor).coerceIn(0f, scrollState.maxValue.toFloat())
            } else 0f
            onWordRange(span.start until span.end)
            scrollState.animateScrollTo(target.toInt())
            delay(intervalMs.toLong())
        }
    }
}
