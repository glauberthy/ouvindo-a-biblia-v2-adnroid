package br.app.ide.ouvindoabiblia.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    // O ID do livro será o slug, ex: "genesis", "mateus"
    @PrimaryKey
    @ColumnInfo(name = "book_id")
    val bookId: String,
    val numericId: Int,
    @ColumnInfo(name = "name")
    val name: String, // Nome formatado: "Gênesis"

    @ColumnInfo(name = "testament")
    val testament: String, // "at" ou "nt"

    @ColumnInfo(name = "folder_path")
    val folderPath: String, // ex: "01-genesis"

    @ColumnInfo(name = "image_url")
    val imageUrl: String?, // Pode ser nulo

    @ColumnInfo(name = "total_chapters")
    val totalChapters: Int
)