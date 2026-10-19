package com.floatscript.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.floatscript.app.model.Script

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val body: String,
    val updatedAt: Long
) {
    fun toDomain() = Script(id = id, title = title, body = body, updatedAt = updatedAt)

    companion object {
        fun fromDomain(script: Script) = ScriptEntity(
            id = script.id,
            title = script.title,
            body = script.body,
            updatedAt = script.updatedAt
        )
    }
}
