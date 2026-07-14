package br.app.ide.ouvindoabiblia.data.repository.domain.mapper

import br.app.ide.ouvindoabiblia.data.local.model.FavoriteStudyLessonDto
import br.app.ide.ouvindoabiblia.data.repository.domain.model.FavoriteLesson

internal fun FavoriteStudyLessonDto.toDomain(): FavoriteLesson = FavoriteLesson(
    lesson = lesson.toDomain(),
    studyTitle = studyTitle,
    studyCoverUrl = studyCoverUrl,
    studyAuthor = studyAuthor
)
