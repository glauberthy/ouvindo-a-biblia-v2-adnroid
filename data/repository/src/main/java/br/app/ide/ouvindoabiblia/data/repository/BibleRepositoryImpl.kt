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
    private val dataStore: DataStore<Preferences> // Injetado via Hilt (DataStoreModule)
) : BibleRepository {

    companion object {
        // Chave para salvar a versão do JSON (ex: "2.0")
        private val KEY_BIBLE_VERSION = stringPreferencesKey("bible_data_version")
        private const val TAG = "BibleRepository"

        private val KEY_LAST_CHAPTER_ID = stringPreferencesKey("last_chapter_id")
        private val KEY_LAST_POSITION = longPreferencesKey("last_position_ms")
    }

    override fun getBooks(): Flow<List<BookEntity>> = dao.getAllBooks()

    override fun getChapters(bookId: String): Flow<List<ChapterWithBookInfo>> =
        dao.getChaptersWithBookInfo(bookId)

    override suspend fun getBook(bookId: String): BookEntity? = dao.getBookById(bookId)

    override suspend fun syncBibleData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Obter a versão remota (Download rápido do JSON)
            // Dica: Se o backend suportasse HEAD request, seria ainda mais rápido,
            // mas baixar o JSON index é leve o suficiente.
            val response = api.getBibleIndex()
            val remoteVersion = response.meta.version

            // 2. Verificar estado local
            val preferences = dataStore.data.first()
            val localVersion = preferences[KEY_BIBLE_VERSION]
            val isDbEmpty = dao.getBookCount() == 0

            // 3. SMART SYNC: Se versões batem E banco tem dados, aborta.
            if (localVersion == remoteVersion && !isDbEmpty) {
                Log.i(TAG, "Sync ignorado: Versão $localVersion já está atualizada.")
                return@withContext Result.success(Unit)
            }

            Log.i(TAG, "Iniciando atualização... Local: $localVersion | Remoto: $remoteVersion")

            // 4. Processamento Pesado (CPU Bound) -> Movemos para Default
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

                // Mapeia ambos os testamentos
                mapTestament("at", response.testamentos.antigoTestamento)
                mapTestament("nt", response.testamentos.novoTestamento)

                Pair(books, chapters)
            }

            // 5. Escrita no Banco e Atualização da Versão (IO Bound)
            if (booksToInsert.isNotEmpty()) {
                // Transação atômica (limpa e insere)
                dao.refreshBibleData(booksToInsert, chaptersToInsert)

                // Salva a nova versão para evitar sync futuro
                dataStore.edit { prefs ->
                    prefs[KEY_BIBLE_VERSION] = remoteVersion
                }

                Log.i(TAG, "Sync concluído com sucesso! Versão atualizada para: $remoteVersion")
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro durante o sync: ${e.message}", e)

            // Se o banco já tem dados, o app continua funcionando offline,
            // então retornamos Failure mas a UI pode decidir não bloquear o uso.
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(chapterId: Long, isFavorite: Boolean) {
        dao.updateFavoriteStatus(chapterId, isFavorite)
    }

    override suspend fun savePlaybackState(chapterId: String, positionMs: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_CHAPTER_ID] = chapterId
            prefs[KEY_LAST_POSITION] = positionMs
        }
    }

    override fun getLatestPlaybackState(): Flow<PlaybackState?> = dataStore.data
        .map { prefs ->
            // 1. Recupera os IDs básicos do DataStore
            val id = prefs[stringPreferencesKey("last_chapter_id")] ?: return@map null
            val pos = prefs[longPreferencesKey("last_position_ms")] ?: 0L

            // 2. Busca metadados (Título e URL) no Room
            // Como esta função no DAO é 'suspend', o Room lida com a thread,
            // mas o 'map' precisa esperar o resultado.
            val info = dao.getChapterWithBookInfoById(id)

            if (info != null) {
                PlaybackState(
                    chapterId = id,
                    positionMs = pos,
                    audioUrl = info.chapter.audioUrl.toUri(),
                    title = "${info.bookName} ${info.chapter.number}"
                )
            } else {
                null
            }
        }
        .flowOn(Dispatchers.IO)

}