package com.floatscript.app.overlay

import com.floatscript.app.model.ScrollMode
import com.floatscript.app.model.ScriptSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeleprompterStateTest {

    private fun state(countdown: Int = 3) = TeleprompterState(ScriptSettings(countdownSeconds = countdown))

    @Test
    fun startsPausedWithControlsVisible() {
        val s = state()
        assertFalse(s.isPlaying.value)
        assertTrue(s.controlsVisible.value)
        assertEquals(0, s.countdown.value)
    }

    @Test
    fun minimalModeStartsWithControlsHidden() {
        val s = TeleprompterState(ScriptSettings(minimalMode = true))
        assertFalse(s.controlsVisible.value)
        s.toggleControls()
        assertTrue(s.controlsVisible.value)
    }

    @Test
    fun playStartsACountdownWhenConfigured() {
        val s = state(countdown = 3)
        s.play()
        assertEquals(3, s.countdown.value)
        assertFalse(s.isPlaying.value)
    }

    @Test
    fun countdownCompletionStartsPlayback() {
        val s = state(countdown = 3)
        s.play()
        s.onCountdownFinished()
        assertEquals(0, s.countdown.value)
        assertTrue(s.isPlaying.value)
    }

    @Test
    fun pauseCancelsTheCountdown() {
        val s = state(countdown = 3)
        s.play()
        s.pause()
        assertEquals(0, s.countdown.value)
        assertFalse(s.isPlaying.value)
        s.onCountdownFinished() // must be a no-op
        assertFalse(s.isPlaying.value)
    }

    @Test
    fun togglePlayDuringCountdownCancelsIt() {
        val s = state(countdown = 5)
        s.play()
        s.togglePlay()
        assertEquals(0, s.countdown.value)
        assertFalse(s.isPlaying.value)
    }

    @Test
    fun playWithoutCountdownStartsImmediately() {
        val s = state(countdown = 0)
        s.play()
        assertTrue(s.isPlaying.value)
        assertEquals(0, s.countdown.value)
    }

    @Test
    fun stopResetsAndBumpsRestartKey() {
        val s = state(countdown = 0)
        s.play()
        val before = s.restartKey.value
        s.stop()
        assertFalse(s.isPlaying.value)
        assertEquals(before + 1, s.restartKey.value)
    }

    @Test
    fun speedIsClampedToRange() {
        val s = state()
        repeat(50) { s.increaseSpeed() }
        assertEquals(ScriptSettings.MAX_SPEED, s.settings.value.scrollSpeedPxPerSec, 0.001f)
        repeat(100) { s.decreaseSpeed() }
        assertEquals(ScriptSettings.MIN_SPEED, s.settings.value.scrollSpeedPxPerSec, 0.001f)
    }

    @Test
    fun fontSizeIsClampedToRange() {
        val s = state()
        repeat(100) { s.increaseTextSize() }
        assertEquals(ScriptSettings.MAX_FONT_SP, s.settings.value.fontSizeSp, 0.001f)
        repeat(200) { s.decreaseTextSize() }
        assertEquals(ScriptSettings.MIN_FONT_SP, s.settings.value.fontSizeSp, 0.001f)
    }

    @Test
    fun scrollModeCyclesInOrder() {
        val s = state()
        assertEquals(ScrollMode.CONTINUOUS, s.settings.value.scrollMode)
        s.cycleScrollMode()
        assertEquals(ScrollMode.LINE_BY_LINE, s.settings.value.scrollMode)
        s.cycleScrollMode()
        assertEquals(ScrollMode.WORD_BY_WORD, s.settings.value.scrollMode)
        s.cycleScrollMode()
        assertEquals(ScrollMode.CONTINUOUS, s.settings.value.scrollMode)
    }

    @Test
    fun setScriptRecordsTheCurrentScript() {
        val s = state()
        s.setScript(7L, "Title", "Body")
        assertEquals(7L, s.currentScriptId)
        assertEquals("Body", s.body.value)
        assertEquals("Title", s.title.value)
    }

    @Test
    fun restartJumpsToTopAndPlaysAgain() {
        val s = state(countdown = 0)
        val before = s.restartKey.value
        s.restart()
        assertEquals(before + 1, s.restartKey.value)
        assertTrue(s.isPlaying.value)
    }
}
