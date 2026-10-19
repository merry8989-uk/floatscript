package com.floatscript.app.data

import com.floatscript.app.model.Script
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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
