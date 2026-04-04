package br.app.ide.ouvindoabiblia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "more_content")
data class MoreContentEntity(
    @PrimaryKey val id: Int = 1,
    val json: String,
    val lastUpdated: String,
    val version: String,
    val fetchedAt: Long = System.currentTimeMillis()
)