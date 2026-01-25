package br.app.ide.ouvindoabiblia.data.repository

import br.app.ide.ouvindoabiblia.data.local.entity.BookEntity
import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo
import kotlinx.coroutines.flow.Flow

interface BibleRepository {
    // Sincroniza dados da API para o Banco (se necessário)
    suspend fun syncBibleData(): Result<Unit>

    // Observa os livros do Banco de Dados
    fun getBooks(): Flow<List<BookEntity>>

    // Observa os capítulos de um livro
//    fun getChapters(bookId: String): Flow<List<ChapterEntity>>
    fun getChapters(bookId: String): Flow<List<ChapterWithBookInfo>>

    // Busca um livro específico
    suspend fun getBook(bookId: String): BookEntity?

    suspend fun toggleFavorite(chapterId: Long, isFavorite: Boolean)
}