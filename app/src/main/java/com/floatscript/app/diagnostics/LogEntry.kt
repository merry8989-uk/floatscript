package com.floatscript.app.diagnostics

/** One line in the diagnostics log. */
data class LogEntry(
    val time: String,
    val level: Char,
    val tag: String,
    val message: String
) {
    fun format(): String = "$time $level/$tag: $message"
}
