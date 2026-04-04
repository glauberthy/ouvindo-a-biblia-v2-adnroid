package br.app.ide.ouvindoabiblia.data.local.model

import androidx.room.Embedded
import br.app.ide.ouvindoabiblia.data.local.entity.StudyLessonEntity

data class FavoriteStudyLessonDto(
    @Embedded val lesson: StudyLessonEntity,
    val studyTitle: String,
    val studyCoverUrl: String,
    val studyAuthor: String
)