package br.app.ide.ouvindoabiblia.data.repository

import android.util.Log
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import br.app.ide.ouvindoabiblia.data.local.dao.BibleDao
import br.app.ide.ouvindoabiblia.data.local.entity.BookEntity
import br.app.ide.ouvindoabiblia.data.local.entity.ChapterEntity
import br.app.ide.ouvindoabiblia.data.local.entity.MomentEntity
import br.app.ide.ouvindoabiblia.data.local.entity.PlaybackStateEntity
import br.app.ide.ouvindoabiblia.data.local.entity.ThemeEntity
import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo
import br.app.ide.ouvindoabiblia.data.remote.api.BibleApi
import br.app.ide.ouvindoabiblia.data.remote.dto.BookDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BibleRepositoryImpl @Inject constructor(
    private val api: BibleApi,
    private val dao: BibleDao,
    private val dataStore: DataStore<Preferences>
) : BibleRepository {

    companion object {
        private const val TAG = "BibleRepository"
        private val KEY_BIBLE_VERSION = stringPreferencesKey("bible_data_version")
        private val KEY_THEMES_VERSION = stringPreferencesKey("themes_data_version")
    }

    override fun getBooks(): Flow<List<BookEntity>> = dao.getAllBooks()

    // O bookId aqui continua sendo o SLUG ("genesis"), o DAO faz o JOIN internamente
    override fun getChapters(bookId: Int): Flow<List<ChapterWithBookInfo>> =
        dao.getChaptersWithBookInfo(bookId)

    override suspend fun getBook(bookId: Int): BookEntity? = dao.getBookById(bookId.toString())

    override suspend fun toggleFavorite(chapterId: Long, isFavorite: Boolean) {
        dao.updateFavoriteStatus(chapterId, isFavorite)
    }

    override suspend fun savePlaybackState(
        chapterId: String,
        positionMs: Long,
        duration: Long,
        title: String,
        subtitle: String,
        imageUrl: String?,
        audioUrl: String
    ) {
        val idLong = chapterId.toLongOrNull() ?: return
        val entity = PlaybackStateEntity(chapterId = idLong, positionMs = positionMs)
        dao.savePlaybackState(entity)
    }

    override fun getLatestPlaybackState(): Flow<PlaybackState?> {
        return dao.getLastPlaybackState()
            .map { dto ->
                if (dto == null) null else {
                    PlaybackState(
                        // SINALIZAÇÃO: Convertendo Long do banco para Int do domínio
                        chapterId = dto.chapterId.toInt(),
                        positionMs = dto.positionMs,
                        duration = 0L,
                        title = "${dto.bookName} ${dto.chapterNumber}",
                        subtitle = "Capítulo ${dto.chapterNumber}",
                        imageUrl = dto.coverUrl,
                        audioUrl = dto.audioUrl.toUri()
                    )
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun clearPlaybackState() {
        dao.clearPlaybackState()
    }

    override suspend fun getBookNumericIdFromChapter(chapterId: Int): Int? {
        return try {
            val chapter = dao.getChapterById(chapterId.toLong())
            chapter?.bookId // Retorna o Int (numericId)
        } catch (e: Exception) {
            null
        }
    }


    override suspend fun getBookIdFromChapter(chapterId: Int): Int? {
        return try {
            // 1. chapterId já é Int, apenas convertemos para Long para o DAO se necessário
            val chapter = dao.getChapterById(chapterId.toLong()) ?: return null

            // 2. Retornamos o bookId da ChapterEntity, que já é o numericId (Int)
            // Não precisamos mais buscar o slug ("genesis"), o número (ex: 6) é o que o Player precisa.
            chapter.bookId
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar ID do livro: ${e.message}")
            null
        }
    }

    // --- CORREÇÃO NO SYNC (MAPEAMENTO) ---
    override suspend fun syncBibleData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getBibleIndex()
            val remoteVersion = response.meta.version
            val preferences = dataStore.data.first()
            val localVersion = preferences[KEY_BIBLE_VERSION]
            val isDbEmpty = dao.getBookCount() == 0

            if (localVersion == remoteVersion && !isDbEmpty) return@withContext Result.success(Unit)

            val (booksToInsert, chaptersToInsert) = withContext(Dispatchers.Default) {
                val books = mutableListOf<BookEntity>()
                val chapters = mutableListOf<ChapterEntity>()

                fun mapTestament(testamentCode: String, booksMap: Map<String, BookDto>) {
                    booksMap.forEach { (slug, dto) ->
                        books.add(
                            BookEntity(
                                numericId = dto.id,    // PK agora é numérica
                                bookId = slug,         // Slug para navegação
                                name = dto.name,
                                testament = testamentCode,
                                folderPath = dto.path,
                                imageUrl = dto.imageUrl,
                                totalChapters = dto.totalChapters
                            )
                        )

                        dto.capitulos.forEach { chapterDto ->
                            chapters.add(
                                ChapterEntity(
                                    // IMPORTANTE: bookId aqui agora é Int (numericId)
                                    bookId = dto.id,
                                    number = chapterDto.numero,
                                    audioUrl = chapterDto.url,
                                    filename = chapterDto.arquivo
                                    // id é auto-gerado (0) e isFavorite é false por padrão
                                )
                            )
                        }
                    }
                }

                mapTestament("at", response.testamentos.antigoTestamento)
                mapTestament("nt", response.testamentos.novoTestamento)
                Pair(books, chapters)
            }

            if (booksToInsert.isNotEmpty()) {
                dao.refreshBibleData(booksToInsert, chaptersToInsert)
                dataStore.edit { it[KEY_BIBLE_VERSION] = remoteVersion }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getFavorites(): Flow<List<ChapterWithBookInfo>> = dao.getFavoriteChapters()
    override fun getChapterByIdFlow(chapterId: Long): Flow<ChapterEntity?> =
        dao.getChapterByIdFlow(chapterId)


    override suspend fun syncThemes(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getThemes()
            val remoteVersion = response.meta.version
            val preferences = dataStore.data.first()
            val localVersion = preferences[KEY_THEMES_VERSION]

            // Só atualiza se a versão for diferente
            if (localVersion == remoteVersion) return@withContext Result.success(Unit)

            val (themesToInsert, momentsToInsert) = withContext(Dispatchers.Default) {
                val themes = mutableListOf<ThemeEntity>()
                val moments = mutableListOf<MomentEntity>()

                response.themes.forEach { themeDto ->
                    themes.add(
                        ThemeEntity(
                            id = themeDto.id,
                            title = themeDto.title,
                            description = themeDto.description,
                            imageUrl = themeDto.imageUrl
                        )
                    )

                    themeDto.moments.forEach { momentDto ->
                        moments.add(
                            MomentEntity(
                                themeId = themeDto.id,
                                bookId = momentDto.bookId,
                                chapterNumber = momentDto.chapter,
                                title = momentDto.title,
                                startMs = momentDto.startMs,
                                endMs = momentDto.endMs,
                                reference = momentDto.reference
                            )
                        )
                    }
                }
                Pair(themes, moments)
            }

            // SINALIZAÇÃO: Chamada ao DAO para atualizar (seguindo padrão do refreshBibleData)
            dao.refreshThemesData(themesToInsert, momentsToInsert)
            dataStore.edit { it[KEY_THEMES_VERSION] = remoteVersion }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}