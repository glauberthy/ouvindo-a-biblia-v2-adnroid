package br.app.ide.ouvindoabiblia.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import br.app.ide.ouvindoabiblia.data.local.entity.StudyEntity
import br.app.ide.ouvindoabiblia.data.local.entity.StudyLessonEntity

data class StudyWithLessons(
    @Embedded val study: StudyEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "studyId"
    )
    val lessons: List<StudyLessonEntity>
)