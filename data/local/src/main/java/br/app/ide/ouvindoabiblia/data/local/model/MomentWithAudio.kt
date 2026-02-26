package br.app.ide.ouvindoabiblia.data.local.model

import androidx.room.Embedded
import br.app.ide.ouvindoabiblia.data.local.entity.MomentEntity

data class MomentWithAudio(
    @Embedded val moment: MomentEntity,
    val audioUrl: String,
    val bookName: String
)