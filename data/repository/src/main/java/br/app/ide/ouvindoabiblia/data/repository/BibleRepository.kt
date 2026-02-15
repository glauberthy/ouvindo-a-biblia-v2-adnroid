package br.app.ide.ouvindoabiblia.data.repository

import android.net.Uri
import br.app.ide.ouvindoabiblia.data.local.entity.BookEntity
import br.app.ide.ouvindoabiblia.data.local.entity.ChapterEntity
import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo
import kotlinx.coroutines.flow.Flow

interface BibleRepository {
    suspend fun syncBibleData(): Result<Unit>
    fun getBooks(): Flow<List<BookEntity>>
    fun getChapters(bookId: Int): Flow<List<ChapterWithBookInfo>>
    suspend fun getBook(bookId: Int): BookEntity?
    suspend fun getBookNumericIdFromChapter(chapterId: Int): Int?
    suspend fun getBookIdFromChapter(chapterId: Int): Int?

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
    fun getLatestPlaybackState(): Flow<PlaybackState?>
    fun getFavorites(): Flow<List<ChapterWithBookInfo>>
    fun getChapterByIdFlow(chapterId: Long): Flow<ChapterEntity?>
}

// Domain Model (Mantido para uso na UI/Service)
data class PlaybackState(
    val chapterId: Int,
    val positionMs: Long,
    val duration: Long,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val audioUrl: Uri
)