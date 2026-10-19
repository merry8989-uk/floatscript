package com.floatscript.app.model

/** Domain model for a saved script (mapped from the Room entity). */
data class Script(
    val id: Long = 0L,
    val title: String,
    val body: String,
    val updatedAt: Long = System.currentTimeMillis()
)
