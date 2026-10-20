package com.floatscript.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {

    @Query("SELECT * FROM scripts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScriptEntity?

    @Query("SELECT COUNT(*) FROM scripts")
    suspend fun count(): Int

    @Query("SELECT * FROM scripts ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ScriptEntity>

    @Insert
    suspend fun insert(entity: ScriptEntity): Long

    @Update
    suspend fun update(entity: ScriptEntity)

    @Delete
    suspend fun delete(entity: ScriptEntity)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
