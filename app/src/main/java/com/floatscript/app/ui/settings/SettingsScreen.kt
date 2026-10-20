package com.floatscript.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floatscript.app.R
import com.floatscript.app.model.ScrollMode
import com.floatscript.app.model.ScriptSettings
import com.floatscript.app.model.TextAlign
import com.floatscript.app.viewmodel.AppViewModelProvider
import com.floatscript.app.viewmodel.SettingsViewModel

private val TEXT_COLORS = listOf(
    Color.White, Color(0xFFFFD54F), Color(0xFF81C784), Color(0xFF4C8DFF),
    Color(0xFFFF8A65), Color.Black
)
private val BG_COLORS = listOf(
    Color.Black, Color(0xFF101418), Color(0xFF1B3A5B), Color(0xFF2E7D32),
    Color(0xFF4A148C), Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Speech-following needs the microphone, so ask for it the moment the user
    // switches the feature on instead of letting the recogniser fail silently.
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setSpeechFollow(granted)
        if (!granted) {
            Toast.makeText(context, context.getString(R.string.speech_permission_needed), Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle(stringResource(R.string.settings_appearance))

            SliderRow(
                label = stringResource(R.string.settings_text_size),
                value = s.fontSizeSp,
                range = ScriptSettings.MIN_FONT_SP..ScriptSettings.MAX_FONT_SP,
                valueText = "${s.fontSizeSp.toInt()} sp",
                onChange = viewModel::setFontSize
            )

            SliderRow(
                label = stringResource(R.string.settings_line_spacing),
                value = s.lineHeightMultiplier,
                range = 1.0f..2.2f,
                valueText = String.format("%.2f", s.lineHeightMultiplier),
                onChange = viewModel::setLineHeight
            )

            SliderRow(
                label = stringResource(R.string.settings_bg_opacity),
                value = s.backgroundOpacity,
                range = 0f..1f,
                valueText = "${(s.backgroundOpacity * 100).toInt()}%",
                onChange = viewModel::setBackgroundOpacity
            )

            SliderRow(
                label = stringResource(R.string.settings_margin),
                value = s.horizontalMarginDp.toFloat(),
                range = 0f..64f,
                valueText = "${s.horizontalMarginDp} dp",
                onChange = { viewModel.setHorizontalMargin(it.toInt()) }
            )

            ColorRow(
                label = stringResource(R.string.settings_text_color),
                colors = TEXT_COLORS,
                selected = s.textColor,
                onSelect = { viewModel.setTextColor(it.toArgb()) }
            )

            ColorRow(
                label = stringResource(R.string.settings_bg_tint),
                colors = BG_COLORS,
                selected = s.backgroundColor,
                onSelect = { viewModel.setBackgroundColor(it.toArgb()) }
            )

            ToggleRow(stringResource(R.string.settings_bold), s.bold, viewModel::setBold)

            ChipRow(
                label = stringResource(R.string.settings_alignment),
                options = TextAlign.entries.map { it.name.lowercase() },
                selectedIndex = TextAlign.entries.indexOf(s.align),
                onSelect = { viewModel.setAlign(TextAlign.entries[it]) }
            )

            SectionTitle(stringResource(R.string.settings_scrolling))

            SliderRow(
                label = stringResource(R.string.settings_speed),
                value = s.scrollSpeedPxPerSec,
                range = ScriptSettings.MIN_SPEED..ScriptSettings.MAX_SPEED,
                valueText = "${s.scrollSpeedPxPerSec.toInt()} px/s",
                onChange = viewModel::setSpeed
            )

            ChipRow(
                label = stringResource(R.string.settings_scroll_mode),
                options = ScrollMode.entries.map { it.name.lowercase().replace('_', ' ') },
                selectedIndex = ScrollMode.entries.indexOf(s.scrollMode),
                onSelect = { viewModel.setScrollMode(ScrollMode.entries[it]) }
            )

            SliderRow(
                label = stringResource(R.string.settings_countdown),
                value = s.countdownSeconds.toFloat(),
                range = 0f..10f,
                valueText = "${s.countdownSeconds} s",
                onChange = { viewModel.setCountdown(it.toInt()) }
            )

            ToggleRow(stringResource(R.string.settings_hide_controls), s.autoHideControls, viewModel::setAutoHide)

            SectionTitle(stringResource(R.string.settings_advanced))

            ToggleRow(stringResource(R.string.settings_mirror_h), s.mirrorHorizontally, viewModel::setMirror)
            ToggleRow(stringResource(R.string.settings_flip_v), s.flipVertically, viewModel::setFlip)
            ToggleRow(stringResource(R.string.settings_keep_awake), s.keepScreenAwake, viewModel::setKeepAwake)
            ToggleRow(
                label = stringResource(R.string.settings_speech_follow),
                checked = s.speechFollowEnabled,
                onChange = { enabled ->
                    when {
                        !enabled -> viewModel.setSpeechFollow(false)
                        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED -> viewModel.setSpeechFollow(true)
                        else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )
            ToggleRow(stringResource(R.string.settings_minimal_mode), s.minimalMode, viewModel::setMinimal)

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ColorRow(
    label: String,
    colors: List<Color>,
    selected: Color,
    onSelect: (Color) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colors.forEach { color ->
                val isSelected = color.toArgb() == selected.toArgb()
                Box(color, isSelected) { onSelect(color) }
            }
        }
    }
}

@Composable
private fun Box(color: Color, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ChipRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { index, option ->
                FilterChip(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    label = { Text(option) }
                )
            }
        }
    }
}
