package br.app.ide.ouvindoabiblia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 1, // ID fixo para garantir apenas 1 registro (Singleton)
    val mediaId: String,         // Mudamos para String! Assim aceita "15" ou "study_12"
    val positionMs: Long,
    val duration: Long,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val audioUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)