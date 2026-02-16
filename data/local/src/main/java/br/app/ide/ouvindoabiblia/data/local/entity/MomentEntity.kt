package br.app.ide.ouvindoabiblia.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "moments",
    foreignKeys = [
        ForeignKey(
            entity = ThemeEntity::class,
            parentColumns = ["id"],
            childColumns = ["themeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["themeId"])]
)
data class MomentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val themeId: Int,
    @ColumnInfo(name = "book_id") val bookId: Int,
    @ColumnInfo(name = "chapter_number") val chapterNumber: Int,
    val title: String,
    @ColumnInfo(name = "start_ms") val startMs: Long,
    @ColumnInfo(name = "end_ms") val endMs: Long,
    val reference: String
)