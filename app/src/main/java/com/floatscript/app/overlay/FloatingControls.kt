package com.floatscript.app.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floatscript.app.R
import com.floatscript.app.model.ScrollMode
import com.floatscript.app.ui.icons.AppIcons

/**
 * The compact, touch-friendly control strip that floats with the script:
 * play/pause, stop, speed, text size, reading mode, restart, hide and close,
 * plus a drag handle and a small read-out of the current values.
 */
@Composable
fun FloatingControls(
    isPlaying: Boolean,
    speed: Float,
    fontSizeSp: Float,
    scrollMode: ScrollMode,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onSpeedDown: () -> Unit,
    onSpeedUp: () -> Unit,
    onTextDown: () -> Unit,
    onTextUp: () -> Unit,
    onCycleMode: () -> Unit,
    onRestart: () -> Unit,
    onHide: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xCC1B1F24), RoundedCornerShape(24.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Live read-out so the user can see the effect of the +/- buttons.
        Text(
            text = "${speed.toInt()} px/s \u00B7 ${fontSizeSp.toInt()} sp \u00B7 ${scrollMode.shortLabel()}",
            color = Color(0xFFB9C2CC),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle — only this area moves the window, so button taps are safe.
            Icon(
                imageVector = AppIcons.DragHandle,
                contentDescription = stringResource(R.string.cd_drag_handle),
                tint = Color(0xFFB9C2CC),
                modifier = Modifier
                    .size(38.dp)
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
            )

            ControlButton(
                icon = if (isPlaying) AppIcons.Pause else Icons.Filled.PlayArrow,
                desc = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                onClick = onTogglePlay
            )
            ControlButton(AppIcons.Stop, stringResource(R.string.cd_stop), onStop)
            ControlButton(AppIcons.Minus, stringResource(R.string.cd_speed_down), onSpeedDown)
            ControlButton(Icons.Filled.Add, stringResource(R.string.cd_speed_up), onSpeedUp)
            ControlButton(Icons.Filled.KeyboardArrowDown, stringResource(R.string.cd_text_down), onTextDown)
            ControlButton(Icons.Filled.KeyboardArrowUp, stringResource(R.string.cd_text_up), onTextUp)
            ModeButton(scrollMode, onCycleMode)
            ControlButton(Icons.Filled.Refresh, stringResource(R.string.cd_restart), onRestart)
            ControlButton(Icons.Filled.Clear, stringResource(R.string.cd_toggle_controls), onHide)
            ControlButton(Icons.Filled.Close, stringResource(R.string.cd_close), onClose)
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Icon(imageVector = icon, contentDescription = desc, tint = Color.White)
    }
}

/** Cycles continuous -> line-by-line -> word-by-word and shows the current mode. */
@Composable
private fun ModeButton(scrollMode: ScrollMode, onClick: () -> Unit) {
    val label = scrollMode.shortLabel()
    val desc = stringResource(R.string.cd_scroll_mode) + ": " + label
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .semantics { contentDescription = desc }
    ) {
        Text(text = label, color = Color.White, fontSize = 11.sp)
    }
}

internal fun ScrollMode.shortLabel(): String = when (this) {
    ScrollMode.CONTINUOUS -> "Cont"
    ScrollMode.LINE_BY_LINE -> "Line"
    ScrollMode.WORD_BY_WORD -> "Word"
}
