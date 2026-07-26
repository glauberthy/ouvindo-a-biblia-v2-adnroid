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
import br.app.ide.ouvindoabiblia.data.local.entity.MoreContentEntity
import br.app.ide.ouvindoabiblia.data.local.entity.PlaybackStateEntity
import br.app.ide.ouvindoabiblia.data.local.entity.StudyEntity
import br.app.ide.ouvindoabiblia.data.local.entity.StudyLessonEntity
import br.app.ide.ouvindoabiblia.data.local.entity.ThemeEntity
import br.app.ide.ouvindoabiblia.data.remote.api.BibleApi
import br.app.ide.ouvindoabiblia.data.remote.dto.BookDto
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreContentDto
import br.app.ide.ouvindoabiblia.data.repository.domain.Resource
import br.app.ide.ouvindoabiblia.data.repository.domain.mapper.toDomain
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Book
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Chapter
import br.app.ide.ouvindoabiblia.data.repository.domain.model.FavoriteLesson
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Lesson
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Moment
import br.app.ide.ouvindoabiblia.data.repository.domain.model.MoreContent
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Study
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
        private val KEY_STUDIES_VERSION = stringPreferencesKey("studies_data_version")
    }

    override fun getBooks(): Flow<List<Book>> =
        dao.getAllBooks().map { list -> list.map { it.toDomain() } }

    override fun getBooksResource(): Flow<Resource<List<Book>>> =
        syncedListResource(getBooks(), ::syncBibleData)

    /**
     * ISSUE 3.B — orquestração centralizada de loading.
     *
     * Encapsula o padrão antes duplicado em Home/Themes/Studies VMs:
     * 1. lê o cache 1x; se vazio -> emite [Resource.Loading];
     * 2. dispara [sync] **uma única vez** (dentro do flow{}, não em combine);
     * 3. reflete o cache: cheio -> Success; vazio+falha -> Error; vazio+ok -> Success([]),
     *    deixando a VM decidir como renderizar lista vazia (Empty/mensagem).
     *
     * O disparo 1x-por-load vem de o [sync] estar no corpo do flow{}: só roda na
     * coleta inicial do upstream. Combinado com stateIn(WhileSubscribed(5s)) na VM,
     * o stream é compartilhado e só reinicia após 5s sem coletores.
     *
     * BUG A (cold start lento): é CACHE-FIRST. Se o Room já tem dados, emite-os
     * IMEDIATAMENTE (Success) e só então dispara o [sync] — que roda depois e apenas
     * reescreve a UI se reescrever o Room (version-gated). Antes o primeiro Success
     * ficava bloqueado atrás do [sync]: em rede fria isso eram 5-10s de Loading sobre
     * um cache já pronto (medido: cache disponível em ~37ms, mas livros só emitidos em
     * ~1550ms, todo o atraso dentro do sync). O caminho de cache vazio é preservado:
     * emite Loading, aguarda o sync e, se o cache continuar vazio, vira Error terminal
     * quando o sync falhou (1ª instalação offline) — nunca Loading eterno.
     */
    private fun <T> syncedListResource(
        cache: Flow<List<T>>,
        sync: suspend () -> Result<Unit>
    ): Flow<Resource<List<T>>> = flow {
        val current = cache.first()
        // Cache-first: com dados locais, mostra já; sem dados, Loading até o sync decidir.
        if (current.isNotEmpty()) emit(Resource.Success(current)) else emit(Resource.Loading)

        val result = sync()

        emitAll(
            cache.map { list ->
                when {
                    list.isNotEmpty() -> Resource.Success(list)
                    // Mensagem humana por causa (ver syncErrorMessage): o localizedMessage
                    // cru expunha "HTTP 429 Too Many Requests" e afins ao usuário.
                    result.isFailure -> Resource.Error(
                        syncErrorMessage(result.exceptionOrNull())
                    )
                    else -> Resource.Success(list) // vazio legítimo: a VM decide (Empty/mensagem)
                }
            }
        )
    }

    // O bookId aqui continua sendo o SLUG ("genesis"), o DAO faz o JOIN internamente
    override fun getChapters(bookId: Int): Flow<List<Chapter>> =
        dao.getChaptersWithBookInfo(bookId).map { list -> list.map { it.toDomain() } }

    override suspend fun toggleFavorite(chapterId: Long, isFavorite: Boolean) {
        dao.updateFavoriteStatus(chapterId, isFavorite)
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun savePlaybackState(
        mediaId: String,
        positionMs: Long,
        duration: Long,
        title: String,
        subtitle: String,
        imageUrl: String?,
        audioUrl: String
    ) {
        // Nada de toLongOrNull()! Passamos tudo direto para a entidade Universal.
        val entity = PlaybackStateEntity(
            mediaId = mediaId,
            positionMs = positionMs,
            duration = duration,
            title = title,
            subtitle = subtitle,
            imageUrl = imageUrl,
            audioUrl = audioUrl
        )

        dao.savePlaybackState(entity)
    }

    override fun getLatestPlaybackState(): Flow<PlaybackState?> {
        return dao.getLastPlaybackState()
            .map { entity ->
                if (entity == null) null else {
                    PlaybackState(
                        mediaId = entity.mediaId,
                        positionMs = entity.positionMs,
                        duration = entity.duration,
                        title = entity.title,
                        subtitle = entity.subtitle,
                        imageUrl = entity.imageUrl,
                        audioUrl = entity.audioUrl.toUri()
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

    override fun getFavorites(): Flow<List<Chapter>> =
        dao.getFavoriteChapters().map { list -> list.map { it.toDomain() } }
    override fun getChapterByIdFlow(chapterId: Long): Flow<Chapter?> =
        dao.getChapterByIdFlow(chapterId).map { it?.toDomain() }


    //    themas
    override suspend fun syncThemes(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getThemes()
            val remoteVersion = response.meta.version
            val preferences = dataStore.data.first()
            val localVersion = preferences[KEY_THEMES_VERSION]

            // VERIFICAÇÃO DE SEGURANÇA: O banco de temas está vazio?
            val isDbEmpty = dao.getThemesCount() == 0

            // Só ignora a atualização se a versão for igual E o banco NÃO estiver vazio
            if (localVersion == remoteVersion && !isDbEmpty) {
                return@withContext Result.success(Unit)
            }

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

            // SINALIZAÇÃO: Chamada ao DAO para atualizar
            dao.refreshThemesData(themesToInsert, momentsToInsert)
            dataStore.edit { it[KEY_THEMES_VERSION] = remoteVersion }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getThemes(): Flow<List<Theme>> =
        dao.getAllThemes().map { list -> list.map { it.toDomain() } }

    override fun getThemesResource(): Flow<Resource<List<Theme>>> =
        syncedListResource(getThemes(), ::syncThemes)

    override fun getMomentsForTheme(themeId: Int): Flow<List<Moment>> =
        dao.getMomentsForTheme(themeId).map { list -> list.map { it.toDomain() } }


    //Estudo
    override fun getStudyWithLessons(studyId: Int): Flow<Study> {
        return dao.getStudyWithLessons(studyId).map { it.toDomain() }
    }

    override suspend fun syncStudies(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getStudies()
            val remoteVersion = response.meta.version
            val preferences = dataStore.data.first()
            val localVersion = preferences[KEY_STUDIES_VERSION]

            val isDbEmpty = dao.getStudiesCount() == 0

            if (localVersion == remoteVersion && !isDbEmpty) {
                return@withContext Result.success(Unit)
            }

            val (studiesToInsert, lessonsToInsert) = withContext(Dispatchers.Default) {
                val studies = mutableListOf<StudyEntity>()
                val lessons = mutableListOf<StudyLessonEntity>()

                response.estudos.forEach { studyDto ->
                    studies.add(
                        StudyEntity(
                            id = studyDto.id,
                            title = studyDto.title,
                            author = studyDto.author ?: "Autor Desconhecido",
                            description = studyDto.description,
                            imageUrl = studyDto.imageUrl
                        )
                    )

                    studyDto.audios.forEach { audioDto ->
                        lessons.add(
                            StudyLessonEntity(
                                remoteId = audioDto.id,
                                studyId = studyDto.id,
                                title = audioDto.title,
                                url = audioDto.url,
                                duration = audioDto.duration ?: 0L,
                                description = audioDto.description
                            )
                        )
                    }
                }

                Pair(studies, lessons)
            }

            dao.refreshStudiesData(studiesToInsert, lessonsToInsert)
            dataStore.edit { it[KEY_STUDIES_VERSION] = remoteVersion }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getStudiesWithLessons(): Flow<List<Study>> {
        return dao.getStudiesWithLessons().map { list -> list.map { it.toDomain() } }
    }

    override fun getStudiesResource(): Flow<Resource<List<Study>>> =
        syncedListResource(getStudiesWithLessons(), ::syncStudies)

    override fun getThemeById(themeId: Int): Flow<Theme?> {
        return dao.getThemeById(themeId).map { it?.toDomain() }
    }

    override suspend fun toggleStudyFavorite(studyId: Int, lessonId: Int, isFavorite: Boolean) {
        dao.updateStudyFavoriteStatus(studyId, lessonId, isFavorite)
    }

    override fun getFavoriteStudyLessons(): Flow<List<FavoriteLesson>> {
        return dao.getFavoriteStudyLessons()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getStudyLessonByIdsFlow(
        studyId: Int,
        lessonId: Int
    ): Flow<Lesson?> {
        return dao.getStudyLessonByIdsFlow(studyId, lessonId).map { it?.toDomain() }
    }

    override suspend fun syncMoreContent(): Result<Unit> {
        return runCatching {
            val remote = api.getMoreContent()

            // ISSUE 6.E: version-gating, como syncBibleData/syncThemes/syncStudies. A versão fica
            // na própria entidade (dao.get()?.version); só reescreve o Room se a versão remota
            // mudou. Antes fazia INSERT REPLACE a cada coleta, mesmo sem mudança.
            val cached = dao.get()
            if (cached != null && cached.version == remote.version) return@runCatching

            val rawJson = json.encodeToString(MoreContentDto.serializer(), remote)

            dao.save(
                MoreContentEntity(
                    id = 1,
                    json = rawJson,
                    lastUpdated = remote.lastUpdated,
                    version = remote.version
                )
            )
        }
    }

    override fun getMoreContent(): Flow<MoreContent?> {
        return dao.observe().map { entity ->
            val cachedJson = entity?.json ?: return@map null

            runCatching {
                json.decodeFromString(MoreContentDto.serializer(), cachedJson).toDomain()
            }.getOrNull()
        }
    }

    // ISSUE 3.B — More é nullable-single (não lista): null == "sem cache".
    override fun getMoreContentResource(): Flow<Resource<MoreContent>> = flow {
        if (getMoreContent().first() == null) emit(Resource.Loading)

        val result = syncMoreContent()

        emitAll(
            getMoreContent().map { content ->
                when {
                    content != null -> Resource.Success(content)
                    result.isFailure -> Resource.Error(
                        syncErrorMessage(result.exceptionOrNull())
                    )
                    // ISSUE PUB-04: sync OK mas sem conteúdo (edge raro: cache decodifica p/ null) é
                    // TERMINAL, não transitório. Antes virava Loading eterno sem saída; agora Error
                    // com Retry (paridade com a ISSUE 6.D na Home).
                    else -> Resource.Error("Nenhum conteúdo disponível. Tente novamente.")
                }
            }
        )
    }
}