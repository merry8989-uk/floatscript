package com.floatscript.app.data

import com.floatscript.app.model.Script
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pure JSON encode/decode for the local backup feature. Kept free of Android and
 * Room types so it can be unit tested on the JVM.
 */
object ScriptBackupCodec {

    const val FORMAT_VERSION = 1

    fun encode(scripts: List<Script>, now: Long = System.currentTimeMillis()): String {
        val array = JSONArray()
        scripts.forEach { script ->
            array.put(
                JSONObject()
                    .put("title", script.title)
                    .put("body", script.body)
                    .put("updatedAt", script.updatedAt)
            )
        }
        return JSONObject()
            .put("app", "FloatScript")
            .put("version", FORMAT_VERSION)
            .put("exportedAt", now)
            .put("scripts", array)
            .toString(2)
    }

    /**
     * Returns the scripts in [json], or `null` when the text is not a valid
     * FloatScript backup. An empty `scripts` array yields an empty list.
     */
    fun decode(json: String, now: Long = System.currentTimeMillis()): List<Script>? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val array = root.optJSONArray("scripts") ?: return null
        val scripts = ArrayList<Script>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            scripts.add(
                Script(
                    id = 0L,
                    title = obj.optString("title").ifBlank { "Imported script" },
                    body = obj.optString("body"),
                    updatedAt = obj.optLong("updatedAt", now)
                )
            )
        }
        return scripts
    }
}
