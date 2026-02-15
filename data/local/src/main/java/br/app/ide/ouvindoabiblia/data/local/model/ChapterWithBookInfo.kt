package br.app.ide.ouvindoabiblia.data.local.model

import androidx.room.Embedded
import br.app.ide.ouvindoabiblia.data.local.entity.ChapterEntity

data class ChapterWithBookInfo(
    @Embedded val chapter: ChapterEntity,
    val bookName: String,
    val coverUrl: String?,
    val testament: String = "",      // Valor padrão vazio
    val totalChapters: Int = 0       // Valor padrão zero
)