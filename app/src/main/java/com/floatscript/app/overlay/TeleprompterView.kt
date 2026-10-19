package com.floatscript.app.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floatscript.app.model.ScriptSettings
import com.floatscript.app.model.TextAlign as AlignModel
import com.floatscript.app.speech.SpeechFollower
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

/**
 * Everything that lives inside the floating window: the tinted background, the
 * scrolling script, the drag / resize handles, the control strip and the
 * countdown. Purely presentational — all state comes from [TeleprompterState].
 */
@Composable
fun TeleprompterView(
    state: TeleprompterState,
    onDrag: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onSettingsPersist: (ScriptSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by state.settings.collectAsStateWithLifecycle()
    val body by state.body.collectAsStateWithLifecycle()
    val isPlaying by state.isPlaying.collectAsStateWithLifecycle()
    val countdown by state.countdown.collectAsStateWithLifecycle()
    val controlsVisible by state.controlsVisible.collectAsStateWithLifecycle()
    val restartKey by state.restartKey.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var wordRange by remember { mutableStateOf<IntRange?>(null) }

    // Persist any settings change coming from the floating controls.
    LaunchedEffect(settings) { onSettingsPersist(settings) }

    // Jump back to the top whenever a restart is requested.
    LaunchedEffect(restartKey) { scrollState.scrollTo(0) }

    // Pre-scroll countdown.
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            for (i in countdown downTo 1) {
                state.setCountdown(i)
                delay(1000)
            }
            state.onCountdownFinished()
        }
    }

    // The scroll engine itself. Disabled while speech-following drives the scroll.
    LaunchedEffect(isPlaying, settings.scrollMode, settings.scrollSpeedPxPerSec, layout, settings.speechFollowEnabled) {
        if (isPlaying && !settings.speechFollowEnabled) {
            ScrollEngine(scrollState, settings, layout) { wordRange = it }.run()
        }
    }

    // Optional speech-following: track recognised words and jump the script to
    // the matching position so it advances as the presenter reads aloud.
    // The recogniser callback runs on a background thread and cannot call the
    // suspending scroll APIs, so it posts target offsets through a Channel that
    // this coroutine drains.
    val context = LocalContext.current
    val speechScrollTargets = remember { Channel<Int>(Channel.CONFLATED) }
    LaunchedEffect(settings.speechFollowEnabled, isPlaying, layout, body) {
        val lr = layout
        if (!settings.speechFollowEnabled || !isPlaying || lr == null) return@LaunchedEffect
        val scriptWords = TextTokenizer.words(body)
        var pointer = 0
        val follower = SpeechFollower(
            context = context,
            onWords = { spoken ->
                for (word in spoken) {
                    val target = matchWord(scriptWords, body, word, pointer) ?: continue
                    pointer = target + 1
                    val span = scriptWords[target]
                    val box = runCatching { lr.getBoundingBox(span.start) }.getOrNull() ?: continue
                    wordRange = span.start until span.end
                    speechScrollTargets.trySend(
                        (box.top - scrollState.viewportSize * 0.30f)
                            .coerceIn(0f, scrollState.maxValue.toFloat()).toInt()
                    )
                }
            },
            onError = { /* transient recogniser errors are ignored */ }
        )
        follower.start()
        try {
            for (target in speechScrollTargets) {
                scrollState.animateScrollTo(target)
            }
        } finally {
            follower.stop()
        }
    }

    // Auto-hide the control strip a moment after playback starts.
    LaunchedEffect(isPlaying, controlsVisible, settings.autoHideControls) {
        if (isPlaying && settings.autoHideControls && controlsVisible) {
            delay(3500)
            state.hideControls()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(settings.backgroundColor.copy(alpha = settings.backgroundOpacity))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { state.toggleControls() }
    ) {
        Column(Modifier.fillMaxSize()) {

            // Always-present, subtle drag strip so the window can be moved even
            // when the control strip is hidden.
            if (!settings.minimalMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .background(Color(0x33FFFFFF))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x99FFFFFF))
                    )
                }
            }

            // The scrolling script.
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val annotated = remember(body, wordRange) {
                    buildAnnotatedString {
                        val range = wordRange
                        if (range == null || range.first >= body.length || range.last >= body.length) {
                            append(body)
                        } else {
                            append(body.substring(0, range.first))
                            withStyle(SpanStyle(background = Color(0x66FFD54F))) {
                                append(body.substring(range.first, range.last + 1))
                            }
                            append(body.substring(range.last + 1))
                        }
                    }
                }
                Text(
                    text = annotated,
                    color = settings.textColor,
                    fontSize = settings.fontSizeSp.sp,
                    fontWeight = if (settings.bold) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
                    textAlign = settings.align.toCompose(),
                    onTextLayout = { layout = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .graphicsLayer(
                            scaleX = if (settings.mirrorHorizontally) -1f else 1f,
                            scaleY = if (settings.flipVertically) -1f else 1f
                        )
                        .padding(horizontal = settings.horizontalMarginDp.dp, vertical = 8.dp)
                )

                // Resize grip in the bottom-end corner.
                if (!settings.minimalMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x55FFFFFF))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onResize(dragAmount.x, dragAmount.y)
                                }
                            }
                    )
                }
            }

            // Countdown banner.
            if (countdown > 0) {
                Box(
                    Modifier.fillMaxWidth().padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countdown.toString(),
                        color = Color(0xFFFFD54F),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Control strip.
            if (controlsVisible && !settings.minimalMode) {
                Row(
                    Modifier.fillMaxWidth().padding(6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FloatingControls(
                        isPlaying = isPlaying,
                        onTogglePlay = { state.togglePlay() },
                        onSpeedDown = { state.decreaseSpeed() },
                        onSpeedUp = { state.increaseSpeed() },
                        onTextDown = { state.decreaseTextSize() },
                        onTextUp = { state.increaseTextSize() },
                        onRestart = { state.restart() },
                        onHide = { state.hideControls() },
                        onClose = { state.requestClose() },
                        onDrag = onDrag
                    )
                }
            }
        }
    }
}

/** Finds the next script word (at or after [from]) that matches a spoken word. */
private fun matchWord(spans: List<TextSpan>, text: String, spoken: String, from: Int): Int? {
    val needle = spoken.lowercase().trim().trim('.', ',', '!', '?', ';', ':')
    if (needle.isEmpty()) return null
    for (i in from until spans.size) {
        val candidate = text.substring(spans[i].start, spans[i].end)
            .lowercase().trim('.', ',', '!', '?', ';', ':')
        if (candidate == needle) return i
    }
    return null
}

private fun AlignModel.toCompose(): TextAlign = when (this) {
    AlignModel.START -> TextAlign.Start
    AlignModel.CENTER -> TextAlign.Center
    AlignModel.END -> TextAlign.End
}
