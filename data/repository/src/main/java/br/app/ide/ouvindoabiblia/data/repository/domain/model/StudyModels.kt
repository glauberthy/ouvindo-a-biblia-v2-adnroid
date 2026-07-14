package br.app.ide.ouvindoabiblia.data.repository.domain.model

/** Aula de um estudo (modelo de domínio; substitui StudyLessonEntity na UI). */
data class Lesson(
    val localId: Long,
    val remoteId: Int,
    val studyId: Int,
    val title: String,
    val url: String,
    val duration: Long,
    val isFavorite: Boolean
)

/** Estudo com suas aulas (modelo de domínio; substitui StudyWithLessons na UI). */
data class Study(
    val id: Int,
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String,
    val lessons: List<Lesson>
)
