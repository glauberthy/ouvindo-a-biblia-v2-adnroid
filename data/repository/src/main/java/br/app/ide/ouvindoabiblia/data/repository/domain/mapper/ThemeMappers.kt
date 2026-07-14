package br.app.ide.ouvindoabiblia.data.repository.domain.mapper

import br.app.ide.ouvindoabiblia.data.local.entity.ThemeEntity
import br.app.ide.ouvindoabiblia.data.local.model.MomentWithAudio
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Moment
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Theme

internal fun ThemeEntity.toDomain(): Theme = Theme(
    id = id,
    title = title,
    description = description,
    imageUrl = imageUrl
)

internal fun MomentWithAudio.toDomain(): Moment = Moment(
    id = moment.id,
    themeId = moment.themeId,
    bookId = moment.bookId,
    chapterNumber = moment.chapterNumber,
    title = moment.title,
    startMs = moment.startMs,
    endMs = moment.endMs,
    reference = moment.reference,
    audioUrl = audioUrl,
    bookName = bookName,
    coverUrl = coverUrl
)
