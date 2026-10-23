package com.floatscript.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptSettingsTest {

    @Test
    fun defaultsAreSensible() {
        val s = ScriptSettings()
        assertEquals(28f, s.fontSizeSp, 0.001f)
        assertEquals(60f, s.scrollSpeedPxPerSec, 0.001f)
        assertTrue(s.backgroundOpacity in 0f..1f)
        assertTrue(s.lineHeightMultiplier >= 1f)
        assertTrue(s.horizontalMarginDp >= 0)
    }

    @Test
    fun rangesAreOrdered() {
        assertTrue(ScriptSettings.MIN_FONT_SP < ScriptSettings.MAX_FONT_SP)
        assertTrue(ScriptSettings.MIN_SPEED < ScriptSettings.MAX_SPEED)
    }
}
