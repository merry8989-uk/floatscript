package com.floatscript.app.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.floatscript.app.R
import com.floatscript.app.ui.icons.AppIcons

/**
 * The compact, touch-friendly control strip that floats with the script:
 * play/pause, speed, text size, restart, hide and close, plus a drag handle.
 */
@Composable
fun FloatingControls(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSpeedDown: () -> Unit,
    onSpeedUp: () -> Unit,
    onTextDown: () -> Unit,
    onTextUp: () -> Unit,
    onRestart: () -> Unit,
    onHide: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xCC1B1F24), RoundedCornerShape(24.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        // Drag handle — only this area moves the window, so button taps are safe.
        Icon(
            imageVector = AppIcons.DragHandle,
            contentDescription = stringResource(R.string.cd_drag_handle),
            tint = Color(0xFFB9C2CC),
            modifier = Modifier
                .size(40.dp)
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
        ControlButton(AppIcons.Minus, stringResource(R.string.cd_speed_down), onSpeedDown)
        ControlButton(Icons.Filled.Add, stringResource(R.string.cd_speed_up), onSpeedUp)
        ControlButton(Icons.Filled.KeyboardArrowDown, stringResource(R.string.cd_text_down), onTextDown)
        ControlButton(Icons.Filled.KeyboardArrowUp, stringResource(R.string.cd_text_up), onTextUp)
        ControlButton(Icons.Filled.Refresh, stringResource(R.string.cd_restart), onRestart)
        ControlButton(Icons.Filled.Clear, stringResource(R.string.cd_toggle_controls), onHide)
        ControlButton(Icons.Filled.Close, stringResource(R.string.cd_close), onClose)
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(imageVector = icon, contentDescription = desc, tint = Color.White)
    }
}
