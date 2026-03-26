package br.app.ide.ouvindoabiblia.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class StudyWithLessons(
    @Embedded
    val study: StudyEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "studyId"
    )
    val lessons: List<StudyLessonEntity>
)