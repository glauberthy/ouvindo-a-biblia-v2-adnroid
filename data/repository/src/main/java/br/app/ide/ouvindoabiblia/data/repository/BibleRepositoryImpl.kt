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
import br.app.ide.ouvindoabiblia.data.local.entity.PlaybackStateEntity
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
    private val dataStore: DataStore<Preferences> // Mantido apenas para controle de versão (Sync)
) : BibleRepository {

    companion object {
        private const val TAG = "BibleRepository"

        // Chave de versão para o Sync (DataStore é apropriado aqui)
        private val KEY_BIBLE_VERSION = stringPreferencesKey("bible_data_version")
    }

    // --- LEITURAS DE CONTEÚDO ---
    override fun getBooks(): Flow<List<BookEntity>> = dao.getAllBooks()

    override fun getChapters(bookId: String): Flow<List<ChapterWithBookInfo>> =
        dao.getChaptersWithBookInfo(bookId)

    override suspend fun getBook(bookId: String): BookEntity? = dao.getBookById(bookId)

    override suspend fun toggleFavorite(chapterId: Long, isFavorite: Boolean) {
        dao.updateFavoriteStatus(chapterId, isFavorite)
    }

    // --- PLAYBACK STATE (MIGRAÇÃO PARA ROOM) ---

    override suspend fun savePlaybackState(
        chapterId: String,
        positionMs: Long,
        duration: Long,
        title: String,
        subtitle: String,
        imageUrl: String?,
        audioUrl: String
    ) {
        // Converte o ID String ("105") para Long (105) exigido pelo Banco
        val idLong = chapterId.toLongOrNull() ?: return

        // Cria a entidade. Note que duration/title/image não são salvos aqui,
        // pois serão recuperados via JOIN com as tabelas originais no getLatest.
        val entity = PlaybackStateEntity(
            chapterId = idLong,
            positionMs = positionMs
        )
        dao.savePlaybackState(entity)
    }

    override fun getLatestPlaybackState(): Flow<PlaybackState?> {
        // Observa a query JOIN do DAO. Se o capítulo for apagado, retorna null automaticamente.
        return dao.getLastPlaybackState()
            .map { dto ->
                if (dto == null) {
                    null
                } else {
                    // Mapeia o DTO do banco para o objeto de domínio
                    PlaybackState(
                        chapterId = dto.chapterId.toString(),
                        positionMs = dto.positionMs,
                        duration = 0L, // Duração é re-calculada pelo player ao preparar
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

    // --- UTILITÁRIOS ---

    override suspend fun getBookIdFromChapter(chapterId: String): String? {
        return try {
            val idLong = chapterId.toLongOrNull() ?: return null

            // CORREÇÃO: Usa a busca por ID numérico, não por URL
            val chapter = dao.getChapterById(idLong)

            chapter?.bookId
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar ID do livro: ${e.message}")
            null
        }
    }

    // --- SINCRONIZAÇÃO (Mantido) ---
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
                                    // Removemos 'id' aqui se for autogerado pelo Room (0),
                                    // ou mapeamos se vier da API. Assumindo auto-generate:
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
}