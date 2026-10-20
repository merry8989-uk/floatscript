package com.floatscript.app.data

import com.floatscript.app.model.Script
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single source of truth for scripts. The UI layer only ever sees the domain
 * [Script] type; Room entities stay inside this package.
 */
class ScriptRepository(private val dao: ScriptDao) {

    val scripts: Flow<List<Script>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun get(id: Long): Script? = withContext(Dispatchers.IO) {
        dao.getById(id)?.toDomain()
    }

    suspend fun upsert(script: Script): Long = withContext(Dispatchers.IO) {
        if (script.id == 0L) {
            dao.insert(ScriptEntity.fromDomain(script.copy(updatedAt = System.currentTimeMillis())))
        } else {
            dao.update(ScriptEntity.fromDomain(script.copy(updatedAt = System.currentTimeMillis())))
            script.id
        }
    }

    suspend fun rename(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        dao.getById(id)?.let {
            dao.update(it.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun duplicate(id: Long): Long? = withContext(Dispatchers.IO) {
        dao.getById(id)?.let { original ->
            dao.insert(
                original.copy(
                    id = 0L,
                    title = "${original.title} (copy)",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun delete(script: Script) = withContext(Dispatchers.IO) {
        dao.delete(ScriptEntity.fromDomain(script))
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) { dao.deleteById(id) }

    /**
     * Serialises every script to a small JSON document for local backup.
     * Uses org.json so no extra serialisation dependency is needed.
     */
    suspend fun exportAll(): String = withContext(Dispatchers.IO) {
        val array = JSONArray()
        dao.getAll().forEach { entity ->
            array.put(
                JSONObject()
                    .put("title", entity.title)
                    .put("body", entity.body)
                    .put("updatedAt", entity.updatedAt)
            )
        }
        JSONObject()
            .put("app", "FloatScript")
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("scripts", array)
            .toString(2)
    }

    /**
     * Restores scripts from a backup produced by [exportAll]. Existing scripts
     * are left untouched; imported ones are added as new entries. Returns the
     * number of scripts imported, or -1 if the file could not be parsed.
     */
    suspend fun importAll(json: String): Int = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(json)
            val array = root.optJSONArray("scripts") ?: return@runCatching -1
            var imported = 0
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val title = obj.optString("title").ifBlank { "Imported script" }
                val body = obj.optString("body")
                dao.insert(
                    ScriptEntity(
                        title = title,
                        body = body,
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
                imported++
            }
            imported
        }.getOrDefault(-1)
    }

    /** Inserts the first-run sample script if the database is empty. */
    suspend fun seedIfEmpty(sampleTitle: String, sampleBody: String) = withContext(Dispatchers.IO) {
        if (dao.count() == 0) {
            dao.insert(
                ScriptEntity(
                    title = sampleTitle,
                    body = sampleBody,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
