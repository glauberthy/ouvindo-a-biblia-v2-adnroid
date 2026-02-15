package br.app.ide.ouvindoabiblia.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["numericId"], // Aponta para o ID numérico do Livro
            childColumns = ["book_id"],    // Coluna local (que agora será Int)
            onDelete = ForeignKey.CASCADE
        )
    ],
    // INDICE ÚNICO: Essencial para o Smart Sync funcionar com ID autogerado
    indices = [
        Index(value = ["book_id", "chapter_number"], unique = true)
    ]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "book_id")
    val bookId: Int, // Alterado para Int para relacionar com numericId

    @ColumnInfo(name = "chapter_number")
    val number: Int,

    @ColumnInfo(name = "audio_url")
    val audioUrl: String,

    @ColumnInfo(name = "filename")
    val filename: String,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)