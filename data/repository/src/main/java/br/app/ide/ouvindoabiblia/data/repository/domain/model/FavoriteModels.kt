package br.app.ide.ouvindoabiblia.data.repository.domain.model

/**
 * Aula de estudo favoritada, com dados do estudo pai
 * (modelo de domínio; substitui FavoriteStudyLessonDto na UI).
 */
data class FavoriteLesson(
    val lesson: Lesson,
    val studyTitle: String,
    val studyCoverUrl: String,
    val studyAuthor: String
)
