package com.floatscript.app.overlay

import com.floatscript.app.model.ScrollMode
import com.floatscript.app.model.ScriptSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory state shared between [OverlayService] and the Compose UI that lives
 * inside the floating window. Deliberately framework-free so it can be unit
 * tested without an Android device.
 */
class TeleprompterState(initialSettings: ScriptSettings) {

    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<ScriptSettings> = _settings.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _body = MutableStateFlow("")
    val body: StateFlow<String> = _body.asStateFlow()

    /** Id of the script currently on screen (0 = none). */
    var currentScriptId: Long = 0L
        private set

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** Non-zero while the pre-scroll countdown is running. */
    private val _countdown = MutableStateFlow(0)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    /**
     * Whether the compact control strip is shown. In minimal mode it starts
     * hidden (script only) but a tap on the script brings it back — so minimal
     * mode never traps the user without a way to start or stop playback.
     */
    private val _controlsVisible = MutableStateFlow(!initialSettings.minimalMode)
    val controlsVisible: StateFlow<Boolean> = _controlsVisible.asStateFlow()

    /** Incrementing this forces the scroll engine to jump back to the top. */
    private val _restartKey = MutableStateFlow(0)
    val restartKey: StateFlow<Int> = _restartKey.asStateFlow()

    /** Set true by the UI to request the service close the window. */
    private val _closeRequested = MutableStateFlow(false)
    val closeRequested: StateFlow<Boolean> = _closeRequested.asStateFlow()

    val isCountingDown: Boolean get() = _countdown.value > 0

    fun setScript(id: Long, title: String, body: String) {
        currentScriptId = id
        _title.value = title
        _body.value = body
    }

    /** Applies settings coming from outside the overlay (e.g. the Settings screen). */
    fun applySettings(new: ScriptSettings) {
        _settings.value = new
    }

    fun setCountdown(seconds: Int) { _countdown.value = seconds }

    fun requestClose() { _closeRequested.value = true }

    fun togglePlay() {
        if (_isPlaying.value || isCountingDown) pause() else play()
    }

    fun play() {
        if (isCountingDown) return // already counting down — ignore
        val s = _settings.value
        if (s.countdownSeconds > 0) {
            _countdown.value = s.countdownSeconds
        } else {
            _isPlaying.value = true
        }
    }

    /** Called by the UI when the countdown reaches zero. */
    fun onCountdownFinished() {
        if (!isCountingDown) return // cancelled in the meantime
        _countdown.value = 0
        _isPlaying.value = true
    }

    /** Pause playback and cancel any running countdown. */
    fun pause() {
        _isPlaying.value = false
        _countdown.value = 0
    }

    fun restart() {
        _isPlaying.value = false
        _countdown.value = 0
        _restartKey.update { it + 1 }
        play()
    }

    /** Stop: pause, cancel the countdown and jump back to the top. */
    fun stop() {
        _isPlaying.value = false
        _countdown.value = 0
        _restartKey.update { it + 1 }
    }

    fun toggleControls() { _controlsVisible.update { !it } }
    fun showControls() { _controlsVisible.value = true }
    fun hideControls() { _controlsVisible.value = false }

    fun increaseSpeed() = updateSpeed(+STEP_SPEED)
    fun decreaseSpeed() = updateSpeed(-STEP_SPEED)
    fun increaseTextSize() = updateFontSize(+STEP_FONT)
    fun decreaseTextSize() = updateFontSize(-STEP_FONT)

    private fun updateSpeed(delta: Float) = _settings.update {
        it.copy(scrollSpeedPxPerSec = (it.scrollSpeedPxPerSec + delta).coerceIn(ScriptSettings.MIN_SPEED, ScriptSettings.MAX_SPEED))
    }

    private fun updateFontSize(delta: Float) = _settings.update {
        it.copy(fontSizeSp = (it.fontSizeSp + delta).coerceIn(ScriptSettings.MIN_FONT_SP, ScriptSettings.MAX_FONT_SP))
    }

    /** Cycles continuous -> line-by-line -> word-by-word, straight from the overlay. */
    fun cycleScrollMode() = _settings.update {
        val modes = ScrollMode.entries
        it.copy(scrollMode = modes[(modes.indexOf(it.scrollMode) + 1) % modes.size])
    }

    private companion object {
        const val STEP_SPEED = 15f
        const val STEP_FONT = 2f
    }
}
