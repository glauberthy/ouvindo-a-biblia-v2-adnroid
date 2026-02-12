package br.app.ide.ouvindoabiblia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 1, // ID fixo para garantir apenas 1 registro (Singleton)
    val chapterId: Long,         // O ID do capítulo (Long para bater com a tabela chapters)
    val positionMs: Long,        // Posição em milissegundos
    val timestamp: Long = System.currentTimeMillis() // Opcional: saber quando foi salvo
)