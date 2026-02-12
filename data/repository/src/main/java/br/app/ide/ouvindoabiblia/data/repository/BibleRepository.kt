package br.app.ide.ouvindoabiblia.data.repository

import android.net.Uri
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
    suspend fun getBookIdFromChapter(chapterId: String): String?
    suspend fun savePlaybackState(
        chapterId: String,
        positionMs: Long,
        duration: Long,
        title: String,
        subtitle: String,
        imageUrl: String?,
        audioUrl: String
    )

    suspend fun clearPlaybackState()
    fun getLatestPlaybackState(): Flow<PlaybackState?>
}

data class PlaybackState(
    val chapterId: String,
    val positionMs: Long,
    val duration: Long,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val audioUrl: Uri
)