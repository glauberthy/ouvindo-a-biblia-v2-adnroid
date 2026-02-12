package br.app.ide.ouvindoabiblia.data.repository

import android.util.Log
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import br.app.ide.ouvindoabiblia.data.local.dao.BibleDao
import br.app.ide.ouvindoabiblia.data.local.entity.BookEntity
import br.app.ide.ouvindoabiblia.data.local.entity.ChapterEntity
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

        // Chaves de Preferências
        private val KEY_BIBLE_VERSION = stringPreferencesKey("bible_data_version")

        // Chaves do Playback State (Agora completos)
        private val KEY_LAST_CHAPTER_ID = stringPreferencesKey("last_chapter_id")
        private val KEY_LAST_POSITION = longPreferencesKey("last_position_ms")
        private val KEY_LAST_DURATION = longPreferencesKey("last_duration_ms")
        private val KEY_LAST_TITLE = stringPreferencesKey("last_title")
        private val KEY_LAST_SUBTITLE = stringPreferencesKey("last_subtitle")
        private val KEY_LAST_IMAGE_URL = stringPreferencesKey("last_image_url")
        private val KEY_LAST_AUDIO_URL = stringPreferencesKey("last_audio_url")
    }

    // --- LEITURAS SIMPLES (Mantidas) ---
    override fun getBooks(): Flow<List<BookEntity>> = dao.getAllBooks()

    override fun getChapters(bookId: String): Flow<List<ChapterWithBookInfo>> =
        dao.getChaptersWithBookInfo(bookId)

    override suspend fun getBook(bookId: String): BookEntity? = dao.getBookById(bookId)

    override suspend fun toggleFavorite(chapterId: Long, isFavorite: Boolean) {
        dao.updateFavoriteStatus(chapterId, isFavorite)
    }

    // --- SALVAMENTO COMPLETO (Ajustado) ---
    override suspend fun savePlaybackState(
        chapterId: String,
        positionMs: Long,
        duration: Long,
        title: String,
        subtitle: String,
        imageUrl: String?,
        audioUrl: String
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_CHAPTER_ID] = chapterId
            prefs[KEY_LAST_POSITION] = positionMs
            prefs[KEY_LAST_DURATION] = duration
            prefs[KEY_LAST_TITLE] = title
            prefs[KEY_LAST_SUBTITLE] = subtitle
            prefs[KEY_LAST_IMAGE_URL] = imageUrl ?: ""
            prefs[KEY_LAST_AUDIO_URL] = audioUrl
        }
    }

    // --- LEITURA RÁPIDA (Otimizada para Cold Start) ---
    override fun getLatestPlaybackState(): Flow<PlaybackState?> = dataStore.data
        .map { prefs ->
            val id = prefs[KEY_LAST_CHAPTER_ID] ?: return@map null
            val pos = prefs[KEY_LAST_POSITION] ?: 0L
            val dur = prefs[KEY_LAST_DURATION] ?: 0L
            val title = prefs[KEY_LAST_TITLE] ?: ""
            val subtitle = prefs[KEY_LAST_SUBTITLE] ?: ""
            val img = prefs[KEY_LAST_IMAGE_URL]?.takeIf { it.isNotEmpty() }
            val audio = prefs[KEY_LAST_AUDIO_URL] ?: ""

            // Retorna o objeto completo SEM ir ao banco de dados (Instantâneo)
            PlaybackState(
                chapterId = id,
                positionMs = pos,
                duration = dur,
                title = title,
                subtitle = subtitle,
                imageUrl = img,
                audioUrl = audio.toUri()
            )
        }
        .flowOn(Dispatchers.IO)


    override suspend fun getBookIdFromChapter(chapterId: String): String? {
        return try {
            // Se o seu banco usa Long (padrão Room), converta.
            val idLong = chapterId.toLongOrNull() ?: return null

            // Busca usando o ID Long
            val info = dao.getChapterWithBookInfoById(idLong.toString())
            // Nota: Ajuste acima conforme seu DAO (se pede String ou Long)

            info?.chapter?.bookId
        } catch (e: Exception) {
            null
        }
    }

    // --- SYNC (Mantido Intacto) ---
    override suspend fun syncBibleData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getBibleIndex()
            val remoteVersion = response.meta.version

            val preferences = dataStore.data.first()
            val localVersion = preferences[KEY_BIBLE_VERSION]
            val isDbEmpty = dao.getBookCount() == 0

            if (localVersion == remoteVersion && !isDbEmpty) {
                Log.i(TAG, "Sync ignorado: Versão $localVersion já está atualizada.")
                return@withContext Result.success(Unit)
            }

            Log.i(TAG, "Iniciando atualização... Local: $localVersion | Remoto: $remoteVersion")

            // Processamento em Default (CPU)
            val (booksToInsert, chaptersToInsert) = withContext(Dispatchers.Default) {
                val books = mutableListOf<BookEntity>()
                val chapters = mutableListOf<ChapterEntity>()

                fun mapTestament(testamentCode: String, booksMap: Map<String, BookDto>) {
                    booksMap.forEach { (slug, dto) ->
                        books.add(
                            BookEntity(
                                bookId = slug,
                                numericId = dto.id,
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
                                    bookId = slug,
                                    number = chapterDto.numero,
                                    audioUrl = chapterDto.url,
                                    filename = chapterDto.arquivo
                                )
                            )
                        }
                    }
                }

                mapTestament("at", response.testamentos.antigoTestamento)
                mapTestament("nt", response.testamentos.novoTestamento)

                Pair(books, chapters)
            }

            // Escrita no Banco (IO)
            if (booksToInsert.isNotEmpty()) {
                dao.refreshBibleData(booksToInsert, chaptersToInsert)

                dataStore.edit { prefs ->
                    prefs[KEY_BIBLE_VERSION] = remoteVersion
                }
                Log.i(TAG, "Sync concluído! Versão: $remoteVersion")
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro sync: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun clearPlaybackState() {
        dataStore.edit { preferences ->
            // OPÇÃO A: Se este DataStore for SÓ para o Player, limpe tudo (Mais fácil)
            preferences.clear()
        }
    }
}