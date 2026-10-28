package com.floatscript.app.update

/**
 * Tiny semantic-ish version comparison. Pure Kotlin so it can be unit tested.
 *
 * Handles tags like `v1.2.0`, plain `1.2.0`, and pre-release suffixes such as
 * `1.2.0-beta` (the numeric parts decide, so `1.2.0-beta` == `1.2.0`).
 */
object AppVersion {

    fun parse(version: String): List<Int> =
        Regex("\\d+")
            .findAll(version.trim().removePrefix("v").removePrefix("V"))
            .map { it.value.toIntOrNull() ?: 0 }
            .toList()

    /** True when [remote] is a strictly newer version than [current]. */
    fun isNewer(remote: String, current: String): Boolean {
        val r = parse(remote)
        val c = parse(current)
        if (r.isEmpty()) return false
        for (i in 0 until maxOf(r.size, c.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /** "1.2.0" from a tag such as "v1.2.0". */
    fun normalize(tag: String): String = tag.trim().removePrefix("v").removePrefix("V")
}
