package com.floatscript.app.update

/** A newer release discovered on the update endpoint. */
data class UpdateInfo(
    val version: String,
    val tag: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long
)
