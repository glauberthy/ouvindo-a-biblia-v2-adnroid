package br.app.ide.ouvindoabiblia.data.repository

import android.net.Uri
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Book
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Chapter
import br.app.ide.ouvindoabiblia.data.repository.domain.model.FavoriteLesson
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Lesson
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Moment
import br.app.ide.ouvindoabiblia.data.repository.domain.model.MoreContent
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Study
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Theme
import kotlinx.coroutines.flow.Flow

interface BibleRepository {
    suspend fun syncBibleData(): Result<Unit>
    suspend fun syncThemes(): Result<Unit>
    fun getBooks(): Flow<List<Book>>
    fun getChapters(bookId: Int): Flow<List<Chapter>>
    suspend fun getBook(bookId: Int): Book?
    suspend fun getBookNumericIdFromChapter(chapterId: Int): Int?
    suspend fun getBookIdFromChapter(chapterId: Int): Int?

    // --- INTERAÇÃO ---
    suspend fun toggleFavorite(chapterId: Long, isFavorite: Boolean)

    // --- PLAYBACK STATE (Agora gerenciado via Room) ---
    suspend fun savePlaybackState(
        mediaId: String,
        positionMs: Long,
        duration: Long,
        title: String,
        subtitle: String,
        imageUrl: String?,
        audioUrl: String
    )

    suspend fun clearPlaybackState()
    fun getLatestPlaybackState(): Flow<PlaybackState?>
    fun getFavorites(): Flow<List<Chapter>>
    fun getChapterByIdFlow(chapterId: Long): Flow<Chapter?>

    //temas
    fun getThemes(): Flow<List<Theme>>
    fun getThemeById(themeId: Int): Flow<Theme?>
    fun getMomentsForTheme(themeId: Int): Flow<List<Moment>>


    //Estudo
    fun getStudyWithLessons(studyId: Int): Flow<Study>
    suspend fun syncStudies(): Result<Unit>

    fun getStudiesWithLessons(): Flow<List<Study>>

    // Novo: Alternar favorito de um estudo
    suspend fun toggleStudyFavorite(studyId: Int, lessonId: Int, isFavorite: Boolean)

    // Novo: Fluxo de lições de estudos favoritas
    fun getFavoriteStudyLessons(): Flow<List<FavoriteLesson>>

    fun getStudyLessonByIdsFlow(studyId: Int, lessonId: Int): Flow<Lesson?>


    suspend fun syncMoreContent(): Result<Unit>
    fun getMoreContent(): Flow<MoreContent?>
}

// Domain Model (Mantido para uso na UI/Service)
data class PlaybackState(
    val mediaId: String,
    val positionMs: Long,
    val duration: Long,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val audioUrl: Uri
)

