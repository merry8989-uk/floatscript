package com.floatscript.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCheckerTest {

    private fun release(tag: String, withApk: Boolean = true): String {
        val asset = if (withApk) {
            """{"name":"FloatScript-$tag.apk","browser_download_url":"https://example.com/$tag.apk","size":123}"""
        } else ""
        return """{"tag_name":"$tag","body":"notes $tag","assets":[$asset]}"""
    }

    @Test
    fun picksTheHighestVersionNotTheMostRecentlyPublished() {
        // Regression: a stray v1.0.0 release published after v1.3.0 made
        // /releases/latest report "up to date".
        val json = "[${release("v1.0.0")},${release("v1.3.0")}]"
        assertEquals("1.3.0", UpdateChecker().pickHighest(json)?.version)
    }

    @Test
    fun ignoresReleasesWithoutAnApkAsset() {
        val json = "[${release("v1.9.0", withApk = false)},${release("v1.3.0")}]"
        assertEquals("1.3.0", UpdateChecker().pickHighest(json)?.version)
    }

    @Test
    fun handlesASingleReleaseObject() {
        assertEquals("1.3.0", UpdateChecker().pickHighest(release("v1.3.0"))?.version)
    }

    @Test
    fun returnsNullWhenNothingIsUsable() {
        assertNull(UpdateChecker().pickHighest("[]"))
        assertNull(UpdateChecker().pickHighest("[${release("v2.0.0", withApk = false)}]"))
        assertNull(UpdateChecker().pickHighest("not json at all"))
    }
}
