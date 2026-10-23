package com.floatscript.app.data

import com.floatscript.app.model.Script
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScriptBackupCodecTest {

    @Test
    fun roundTripsScripts() {
        val original = listOf(
            Script(id = 0, title = "One", body = "First body", updatedAt = 1000L),
            Script(id = 0, title = "Two", body = "Second body", updatedAt = 2000L)
        )
        val decoded = ScriptBackupCodec.decode(ScriptBackupCodec.encode(original))
        assertNotNull(decoded)
        assertEquals(2, decoded!!.size)
        assertEquals("One", decoded[0].title)
        assertEquals("First body", decoded[0].body)
        assertEquals(2000L, decoded[1].updatedAt)
    }

    @Test
    fun rejectsTextThatIsNotJson() {
        assertNull(ScriptBackupCodec.decode("not json at all"))
    }

    @Test
    fun rejectsJsonWithoutScriptsArray() {
        assertNull(ScriptBackupCodec.decode("""{"app":"FloatScript"}"""))
    }

    @Test
    fun emptyScriptsArrayDecodesToEmptyList() {
        val decoded = ScriptBackupCodec.decode("""{"scripts":[]}""")
        assertEquals(0, decoded?.size)
    }

    @Test
    fun blankTitleGetsAFallback() {
        val decoded = ScriptBackupCodec.decode("""{"scripts":[{"title":"","body":"x"}]}""")
        assertEquals("Imported script", decoded!![0].title)
    }

    @Test
    fun missingUpdatedAtFallsBackToNow() {
        val decoded = ScriptBackupCodec.decode("""{"scripts":[{"title":"T","body":"B"}]}""", now = 42L)
        assertEquals(42L, decoded!![0].updatedAt)
    }
}
