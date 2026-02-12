package br.app.ide.ouvindoabiblia.data.local.model

import androidx.room.ColumnInfo

// Esta classe serve apenas para receber o resultado da Query complexa do banco
data class PlaybackStateDto(
    @ColumnInfo(name = "chapterId") val chapterId: Long,
    @ColumnInfo(name = "positionMs") val positionMs: Long,
    @ColumnInfo(name = "audioUrl") val audioUrl: String,
    @ColumnInfo(name = "chapterNumber") val chapterNumber: Int,
    @ColumnInfo(name = "bookId") val bookId: String,
    @ColumnInfo(name = "bookName") val bookName: String,
    @ColumnInfo(name = "coverUrl") val coverUrl: String?
)