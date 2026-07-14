package br.app.ide.ouvindoabiblia.data.repository.domain.model

/** Tema (modelo de domínio; substitui ThemeEntity na UI). */
data class Theme(
    val id: Int,
    val title: String,
    val description: String,
    val imageUrl: String?
)

/**
 * Momento de um tema, já com áudio/livro resolvidos
 * (modelo de domínio; substitui MomentWithAudio na UI).
 */
data class Moment(
    val id: Long,
    val themeId: Int,
    val bookId: Int,
    val chapterNumber: Int,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val reference: String,
    val audioUrl: String,
    val bookName: String,
    val coverUrl: String?
)
