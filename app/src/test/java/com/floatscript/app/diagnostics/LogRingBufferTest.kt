package com.floatscript.app.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogRingBufferTest {

    private fun entry(n: Int) = LogEntry(time = "00:00:00.00$n", level = 'I', tag = "T", message = "m$n")

    @Test
    fun keepsEverythingBelowCapacity() {
        val buffer = LogRingBuffer(5)
        repeat(3) { buffer.add(entry(it)) }
        assertEquals(3, buffer.size)
        assertEquals(listOf("m0", "m1", "m2"), buffer.snapshot().map { it.message })
    }

    @Test
    fun dropsTheOldestEntriesWhenFull() {
        val buffer = LogRingBuffer(3)
        repeat(6) { buffer.add(entry(it)) }
        assertEquals(3, buffer.size)
        assertEquals(listOf("m3", "m4", "m5"), buffer.snapshot().map { it.message })
    }

    @Test
    fun clearEmptiesTheBuffer() {
        val buffer = LogRingBuffer(3)
        repeat(3) { buffer.add(entry(it)) }
        buffer.clear()
        assertEquals(0, buffer.size)
        assertTrue(buffer.snapshot().isEmpty())
    }

    @Test
    fun capacityIsAtLeastOne() {
        val buffer = LogRingBuffer(0)
        assertEquals(1, buffer.capacity)
        buffer.add(entry(1))
        assertEquals(1, buffer.size)
    }

    @Test
    fun formatsEntriesWithTimeLevelTagAndMessage() {
        val line = LogEntry("10:00:00.000", 'W', "Overlay", "permission missing").format()
        assertEquals("10:00:00.000 W/Overlay: permission missing", line)
    }
}
