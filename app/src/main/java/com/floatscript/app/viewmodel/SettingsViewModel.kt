package com.floatscript.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floatscript.app.data.SettingsRepository
import com.floatscript.app.model.ScrollMode
import com.floatscript.app.model.ScriptSettings
import com.floatscript.app.model.TextAlign
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Exposes the persisted [ScriptSettings] and mutators used by the settings screen. */
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<ScriptSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScriptSettings())

    private fun update(transform: (ScriptSettings) -> ScriptSettings) {
        viewModelScope.launch { repository.saveSettings(transform(settings.value)) }
    }

    fun setFontSize(value: Float) = update { it.copy(fontSizeSp = value) }
    fun setBold(value: Boolean) = update { it.copy(bold = value) }
    fun setLineHeight(value: Float) = update { it.copy(lineHeightMultiplier = value) }
    fun setAlign(value: TextAlign) = update { it.copy(align = value) }
    fun setBackgroundOpacity(value: Float) = update { it.copy(backgroundOpacity = value) }
    fun setHorizontalMargin(value: Int) = update { it.copy(horizontalMarginDp = value) }
    fun setSpeed(value: Float) = update { it.copy(scrollSpeedPxPerSec = value) }
    fun setScrollMode(value: ScrollMode) = update { it.copy(scrollMode = value) }
    fun setCountdown(value: Int) = update { it.copy(countdownSeconds = value) }
    fun setAutoHide(value: Boolean) = update { it.copy(autoHideControls = value) }
    fun setMirror(value: Boolean) = update { it.copy(mirrorHorizontally = value) }
    fun setFlip(value: Boolean) = update { it.copy(flipVertically = value) }
    fun setKeepAwake(value: Boolean) = update { it.copy(keepScreenAwake = value) }
    fun setSpeechFollow(value: Boolean) = update { it.copy(speechFollowEnabled = value) }
    fun setMinimal(value: Boolean) = update { it.copy(minimalMode = value) }
    fun setTextColor(argb: Int) = update { it.copy(textColor = androidx.compose.ui.graphics.Color(argb)) }
    fun setBackgroundColor(argb: Int) = update { it.copy(backgroundColor = androidx.compose.ui.graphics.Color(argb)) }
}
