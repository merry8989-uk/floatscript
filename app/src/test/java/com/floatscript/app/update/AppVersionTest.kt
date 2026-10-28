package com.floatscript.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun parsesTagsWithAndWithoutPrefix() {
        assertEquals(listOf(1, 2, 0), AppVersion.parse("v1.2.0"))
        assertEquals(listOf(1, 2, 0), AppVersion.parse("1.2.0"))
        assertEquals(listOf(2, 0), AppVersion.parse("2.0"))
    }

    @Test
    fun detectsNewerVersions() {
        assertTrue(AppVersion.isNewer("1.3.0", "1.2.0"))
        assertTrue(AppVersion.isNewer("1.10.0", "1.9.9"))
        assertTrue(AppVersion.isNewer("2", "1.9.9"))
        assertFalse(AppVersion.isNewer("1.2.0", "1.2.0"))
        assertFalse(AppVersion.isNewer("1.1.9", "1.2.0"))
    }

    @Test
    fun prereleaseSuffixIsIgnoredForOrdering() {
        assertFalse(AppVersion.isNewer("1.2.0-beta", "1.2.0"))
        assertTrue(AppVersion.isNewer("1.2.1-beta", "1.2.0"))
    }

    @Test
    fun normalizeStripsThePrefix() {
        assertEquals("1.2.0", AppVersion.normalize("v1.2.0"))
        assertEquals("1.2.0", AppVersion.normalize("V1.2.0"))
    }

    @Test
    fun unparseableVersionsAreNeverNewer() {
        assertFalse(AppVersion.isNewer("nightly", "1.2.0"))
        assertFalse(AppVersion.isNewer("", "1.2.0"))
    }
}
