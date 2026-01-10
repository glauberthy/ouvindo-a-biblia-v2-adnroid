package br.app.ide.ouvindoabiblia.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import br.app.ide.ouvindoabiblia.data.local.entity.ChapterEntity

data class ChapterWithBookInfo(
    // @Embedded pega todos os campos de ChapterEntity e joga aqui dentro
    @Embedded val chapter: ChapterEntity,

    // Aqui pegamos apenas o que precisamos da tabela de Livros (JOIN)
    @ColumnInfo(name = "bookName") val bookName: String,
    @ColumnInfo(name = "coverUrl") val coverUrl: String?
)