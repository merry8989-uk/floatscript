package com.floatscript.app.diagnostics

/**
 * Fixed-size, in-memory log buffer that keeps only the most recent entries.
 * Deliberately free of Android types so it can be unit tested on the JVM.
 *
 * Not thread-safe on its own — [Diagnostics] guards it with a lock.
 */
class LogRingBuffer(capacity: Int) {

    /** Always at least 1, so the buffer never silently keeps nothing. */
    val capacity: Int = capacity.coerceAtLeast(1)

    private val items = ArrayDeque<LogEntry>(this.capacity)

    val size: Int get() = items.size

    fun add(entry: LogEntry) {
        items.addLast(entry)
        while (items.size > this.capacity) items.removeFirst()
    }

    fun snapshot(): List<LogEntry> = items.toList()

    fun clear() = items.clear()
}
