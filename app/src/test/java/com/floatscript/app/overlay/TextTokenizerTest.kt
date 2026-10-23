package com.floatscript.app.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextTokenizerTest {

    @Test
    fun splitsOnSingleSpaces() {
        val text = "hello world"
        val spans = TextTokenizer.words(text)
        assertEquals(2, spans.size)
        assertEquals("hello", text.substring(spans[0].start, spans[0].end))
        assertEquals("world", text.substring(spans[1].start, spans[1].end))
    }

    @Test
    fun collapsesRunsOfWhitespace() {
        val text = "  a \n\t b  "
        val spans = TextTokenizer.words(text)
        assertEquals(2, spans.size)
        assertEquals("a", text.substring(spans[0].start, spans[0].end))
        assertEquals("b", text.substring(spans[1].start, spans[1].end))
    }

    @Test
    fun emptyInputYieldsNothing() {
        assertTrue(TextTokenizer.words("").isEmpty())
        assertTrue(TextTokenizer.words("   \n  ").isEmpty())
    }

    @Test
    fun keepsPunctuationAttached() {
        val text = "Hello, world!"
        val spans = TextTokenizer.words(text)
        assertEquals(2, spans.size)
        assertEquals("Hello,", text.substring(spans[0].start, spans[0].end))
        assertEquals("world!", text.substring(spans[1].start, spans[1].end))
    }

    @Test
    fun rangesAreOrderedAndNonEmpty() {
        val spans = TextTokenizer.words("one two three")
        spans.zipWithNext().forEach { (a, b) -> assertTrue(a.end <= b.start) }
        assertTrue(spans.all { it.start < it.end })
    }
}
