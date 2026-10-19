package com.floatscript.app.overlay

/** A word (or line) with its character range inside the source string. */
data class TextSpan(val start: Int, val end: Int)

/** Splits script text into words / lines for the word- and line-based modes. */
object TextTokenizer {

    /** Words separated by any whitespace, each with its exact char range. */
    fun words(text: String): List<TextSpan> {
        val spans = ArrayList<TextSpan>()
        var i = 0
        val n = text.length
        while (i < n) {
            while (i < n && text[i].isWhitespace()) i++
            if (i >= n) break
            val start = i
            while (i < n && !text[i].isWhitespace()) i++
            spans.add(TextSpan(start, i))
        }
        return spans
    }
}
