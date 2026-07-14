package br.app.ide.ouvindoabiblia.data.repository.domain.model

/** Livro da Bíblia (modelo de domínio; substitui BookEntity na UI). */
data class Book(
    val numericId: Int,
    val bookId: String,
    val name: String,
    val testament: String,
    val folderPath: String,
    val imageUrl: String?,
    val totalChapters: Int
)

/**
 * Capítulo da Bíblia (modelo de domínio; unifica ChapterWithBookInfo e ChapterEntity).
 *
 * Os campos de livro têm default porque a busca "crua" por id
 * (getChapterByIdFlow, usada só para observar [isFavorite]) não carrega o JOIN.
 */
data class Chapter(
    val id: Long,
    val bookId: Int,
    val number: Int,
    val audioUrl: String,
    val isFavorite: Boolean,
    val bookName: String = "",
    val coverUrl: String? = null,
    val testament: String = "",
    val totalChapters: Int = 0
)
