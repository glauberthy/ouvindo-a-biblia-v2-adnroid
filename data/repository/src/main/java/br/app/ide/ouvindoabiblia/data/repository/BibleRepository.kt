package br.app.ide.ouvindoabiblia.data.repository

import android.net.Uri
import br.app.ide.ouvindoabiblia.data.local.entity.BookEntity
import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo
import kotlinx.coroutines.flow.Flow

interface BibleRepository {
    // Sincroniza dados da API
    suspend fun syncBibleData(): Result<Unit>

    // --- LEITURA ---
    fun getBooks(): Flow<List<BookEntity>>
    fun getChapters(bookId: String): Flow<List<ChapterWithBookInfo>>
    suspend fun getBook(bookId: String): BookEntity?
    suspend fun getBookIdFromChapter(chapterId: String): String?

    // --- INTERAÇÃO ---
    suspend fun toggleFavorite(chapterId: Long, isFavorite: Boolean)

    // --- PLAYBACK STATE (Agora gerenciado via Room) ---
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

    // Retorna o estado combinando dados da tabela de playback com a tabela de capítulos/livros
    fun getLatestPlaybackState(): Flow<PlaybackState?>
}

// Domain Model (Mantido para uso na UI/Service)
data class PlaybackState(
    val chapterId: String,
    val positionMs: Long,
    val duration: Long,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val audioUrl: Uri
)