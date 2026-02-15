package br.app.ide.ouvindoabiblia.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    @ColumnInfo(name = "numericId") // Agora o ID numérico (1, 2, 40) é a chave primária
    val numericId: Int,

    @ColumnInfo(name = "book_id") // O slug "genesis" continua aqui para uso no código
    val bookId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "testament")
    val testament: String,

    @ColumnInfo(name = "folder_path")
    val folderPath: String,

    @ColumnInfo(name = "image_url")
    val imageUrl: String?,

    @ColumnInfo(name = "total_chapters")
    val totalChapters: Int
)