package br.app.ide.ouvindoabiblia.data.repository.domain.mapper

import br.app.ide.ouvindoabiblia.data.local.entity.StudyLessonEntity
import br.app.ide.ouvindoabiblia.data.local.model.StudyWithLessons
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Lesson
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Study

internal fun StudyLessonEntity.toDomain(): Lesson = Lesson(
    localId = localId,
    remoteId = remoteId,
    studyId = studyId,
    title = title,
    url = url,
    duration = duration,
    isFavorite = isFavorite,
    description = description
)

internal fun StudyWithLessons.toDomain(): Study = Study(
    id = study.id,
    title = study.title,
    author = study.author,
    description = study.description,
    imageUrl = study.imageUrl,
    lessons = lessons.map { it.toDomain() }
)
