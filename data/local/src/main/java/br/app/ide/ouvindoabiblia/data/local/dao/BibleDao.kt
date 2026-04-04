package br.app.ide.ouvindoabiblia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import br.app.ide.ouvindoabiblia.data.local.entity.BookEntity
import br.app.ide.ouvindoabiblia.data.local.entity.ChapterEntity
import br.app.ide.ouvindoabiblia.data.local.entity.MomentEntity
import br.app.ide.ouvindoabiblia.data.local.entity.MoreContentEntity
import br.app.ide.ouvindoabiblia.data.local.entity.PlaybackStateEntity
import br.app.ide.ouvindoabiblia.data.local.entity.StudyEntity
import br.app.ide.ouvindoabiblia.data.local.entity.StudyLessonEntity
import br.app.ide.ouvindoabiblia.data.local.entity.ThemeEntity
import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo
import br.app.ide.ouvindoabiblia.data.local.model.FavoriteStudyLessonDto
import br.app.ide.ouvindoabiblia.data.local.model.MomentWithAudio
import br.app.ide.ouvindoabiblia.data.local.model.StudyWithLessons
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleDao {

    // --- LEITURA (Usada pela UI) ---

    // Retorna todos os livros. Flow atualiza a UI automaticamente se algo mudar.
    @Query("SELECT * FROM books ORDER BY testament DESC, book_id ASC")
    fun getAllBooks(): Flow<List<BookEntity>>

    // Seleciona tudo do capítulo (*)
    // E pega o nome e imagem da tabela de livros
    // Onde o ID do livro bater entre as duas tabelas.
    @Transaction
    @Query(
        """
    SELECT 
        chapters.*, 
        books.name as bookName, 
        books.image_url as coverUrl, 
        books.testament as testament,
        books.total_chapters as totalChapters
    FROM chapters 
    -- CORREÇÃO: Comparar com numericId (número), não book_id (slug)
    INNER JOIN books ON chapters.book_id = books.numericId 
    WHERE chapters.book_id = :bookId
    ORDER BY chapters.chapter_number ASC
"""
    )
    fun getChaptersWithBookInfo(bookId: Int): Flow<List<ChapterWithBookInfo>>

    // Busca capítulos de um livro específico
    @Query("SELECT * FROM chapters WHERE book_id = :bookId ORDER BY chapter_number ASC")
    fun getChaptersForBook(bookId: Int): Flow<List<ChapterEntity>> // Agora recebe Int

    // Busca um único livro (útil para pegar a capa no player)
    @Query("SELECT * FROM books WHERE book_id = :bookId LIMIT 1")
    suspend fun getBookById(bookId: String): BookEntity?

    // --- ESCRITA (Usada quando o app inicia e baixa o JSON) ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookIgnore(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChapterIgnore(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChaptersIgnore(chapters: List<ChapterEntity>): List<Long>

    // Deleta tudo (útil para resetar dados se o JSON mudar muito)
    @Query("DELETE FROM books")
    suspend fun clearBooks()

    @Query("DELETE FROM chapters")
    suspend fun clearChapters()


    //Verificação rápida para saber se precisa popular o banco inicial
    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    // Uma transação garante que limpa e insere tudo de uma vez com segurança
    @Transaction
    suspend fun refreshBibleData(books: List<BookEntity>, chapters: List<ChapterEntity>) {

        // Sincroniza Livros (Protegendo contra o CASCADE DELETE)
        books.forEach { book ->
            val rowId = insertBookIgnore(book)
            if (rowId == -1L) {
                // Já existe: atualizamos apenas os dados técnicos
                updateBookMetadata(
                    book.numericId,
                    book.name,
                    book.imageUrl,
                    book.folderPath,
                    book.totalChapters
                )
            }
        }

        // Sincroniza Capítulos (Protegendo os Favoritos)
        chapters.forEach { chapter ->
            val rowId = insertChapterIgnore(chapter)
            if (rowId == -1L) {
                // Já existe: atualizamos apenas a URL de áudio sem tocar no is_favorite
                updateChapterMetadataByCompositeKey(
                    bookId = chapter.bookId,
                    chapterNumber = chapter.number,
                    audioUrl = chapter.audioUrl,
                    filename = chapter.filename
                )
            }
        }
    }

    @Query(
        """
        UPDATE books SET name = :name, image_url = :imageUrl, folder_path = :folderPath, total_chapters = :totalChapters 
        WHERE numericId = :numericId
    """
    )
    suspend fun updateBookMetadata(
        numericId: Int,
        name: String,
        imageUrl: String?,
        folderPath: String,
        totalChapters: Int
    )

    // SINALIZAÇÃO: CORREÇÃO CRÍTICA!
    // Como o 'id' no ChapterEntity vindo do Sync é 0, precisamos atualizar pela CHAVE COMPOSTA (Livro + Número)
    @Query(
        """
        UPDATE chapters SET audio_url = :audioUrl, filename = :filename 
        WHERE book_id = :bookId AND chapter_number = :chapterNumber
    """
    )
    suspend fun updateChapterMetadataByCompositeKey(
        bookId: Int,
        chapterNumber: Int,
        audioUrl: String,
        filename: String
    )

    @Query("UPDATE chapters SET is_favorite = :isFavorite WHERE id = :chapterId")
    suspend fun updateFavoriteStatus(chapterId: Long, isFavorite: Boolean)


    // Busca um capítulo específico pelo ID (Mantido para compatibilidade, se usado em outro lugar)
    @Transaction
    @Query(
        """
    SELECT 
        chapters.*, 
        books.name as bookName, 
        books.image_url as coverUrl,
        books.testament as testament,      -- Adicionado
        books.total_chapters as totalChapters -- Adicionado
    FROM chapters 
    INNER JOIN books ON chapters.book_id = books.book_id 
    WHERE chapters.audio_url = :audioUrl OR chapters.book_id = :audioUrl 
    LIMIT 1
"""
    )
    suspend fun getChapterWithBookInfoById(audioUrl: String): ChapterWithBookInfo?


    @Query(
        """
        UPDATE chapters 
        SET audio_url = :audioUrl, 
            filename = :filename,
            book_id = :bookId,
            chapter_number = :chapterNumber
        WHERE id = :id
    """
    )
    suspend fun updateChapterMetadata(
        id: Long,
        audioUrl: String,
        filename: String,
        bookId: Int,
        chapterNumber: Int
    )


    // 1. Salvar o Estado (Substitui se já existir, mantendo sempre o ID=1)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlaybackState(state: PlaybackStateEntity)

    // 2. Limpar o Estado (caso precise resetar)
    @Query("DELETE FROM playback_state")
    suspend fun clearPlaybackState()


    @Query("SELECT * FROM playback_state WHERE id = 1")
    fun getLastPlaybackState(): Flow<PlaybackStateEntity?>

    @Query("SELECT * FROM chapters WHERE id = :chapterId LIMIT 1")
    suspend fun getChapterById(chapterId: Long): ChapterEntity?


    @Transaction
    @Query(
        """
    SELECT 
        chapters.*, 
        books.name as bookName, 
        books.image_url as coverUrl,
        books.testament as testament,
        books.total_chapters as totalChapters
    FROM chapters
    INNER JOIN books ON chapters.book_id = books.numericId 
    WHERE chapters.is_favorite = 1
    ORDER BY books.numericId ASC, CAST(chapters.chapter_number AS INTEGER) ASC
"""
    )
    fun getFavoriteChapters(): Flow<List<ChapterWithBookInfo>>
    // No BibleDao.kt

    @Query("SELECT * FROM chapters WHERE id = :chapterId LIMIT 1")
    fun getChapterByIdFlow(chapterId: Long): Flow<ChapterEntity?>


    // --- TEMAS E MOMENTOS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheme(theme: ThemeEntity)

    @Query("SELECT * FROM themes ORDER BY id ASC")
    fun getAllThemes(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE id = :themeId LIMIT 1")
    fun getThemeById(themeId: Int): Flow<ThemeEntity?>


    @Query("DELETE FROM moments WHERE themeId = :themeId")
    suspend fun deleteMomentsByTheme(themeId: Int)

    @Insert
    suspend fun insertMoments(moments: List<MomentEntity>)

    @Transaction
    suspend fun refreshThemesData(themes: List<ThemeEntity>, moments: List<MomentEntity>) {

        themes.forEach { theme ->
            insertTheme(theme)
        }


        themes.forEach { theme ->
            deleteMomentsByTheme(theme.id)
        }
        insertMoments(moments)
    }

    // Query para buscar os momentos com áudio (usaremos na UI em breve)
    @Transaction
    @Query(
        """
        SELECT 
            M.*, 
            C.audio_url as audioUrl,
            B.name as bookName,
            B.image_url as coverUrl
        FROM moments M
        INNER JOIN chapters C ON M.book_id = C.book_id AND M.chapter_number = C.chapter_number
        INNER JOIN books B ON M.book_id = B.numericId
        WHERE M.themeId = :themeId
    """
    )
    fun getMomentsForTheme(themeId: Int): Flow<List<MomentWithAudio>>

    //Estudos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudies(studies: List<StudyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyLessons(lessons: List<StudyLessonEntity>)

    @Query("SELECT * FROM studies")
    fun getStudies(): Flow<List<StudyEntity>>

    @Transaction
    @Query("SELECT * FROM studies WHERE id = :studyId")
    fun getStudyWithLessons(studyId: Int): Flow<StudyWithLessons>

    @Query("DELETE FROM studies")
    suspend fun clearStudies()

    @Query("DELETE FROM study_lessons")
    suspend fun clearStudyLessons()


    @Query("SELECT COUNT(*) FROM themes")
    suspend fun getThemesCount(): Int

    @Transaction
    @Query("SELECT * FROM studies")
    fun getStudiesWithLessons(): Flow<List<StudyWithLessons>>

    @Query("SELECT COUNT(*) FROM studies")
    suspend fun getStudiesCount(): Int


    @Transaction
    suspend fun refreshStudiesData(
        studies: List<StudyEntity>,
        lessons: List<StudyLessonEntity>
    ) {
        studies.forEach { study ->
            val rowId = insertStudyIgnore(study)
            if (rowId == -1L) {
                updateStudyMetadata(
                    studyId = study.id,
                    title = study.title,
                    author = study.author,
                    imageUrl = study.imageUrl,
                    description = study.description
                )
            }
        }

        lessons.forEach { lesson ->
            val rowId = insertStudyLessonIgnore(lesson)
            if (rowId == -1L) {
                updateStudyLessonMetadata(
                    studyId = lesson.studyId,
                    remoteId = lesson.remoteId,
                    title = lesson.title,
                    url = lesson.url,
                    duration = lesson.duration
                )
            }
        }
    }

    // 1. Alterna o status de favorito de uma lição de estudo
    @Query("UPDATE study_lessons SET isFavorite = :isFavorite WHERE remoteId = :lessonId AND studyId = :studyId")
    suspend fun updateStudyFavoriteStatus(studyId: Int, lessonId: Int, isFavorite: Boolean)

    // 2. Busca todas as lições favoritas, trazendo junto os dados do Estudo (para mostrar a capa e o título do autor)
    @Transaction
    @Query(
        """
    SELECT L.*, S.title as studyTitle, S.image_url as studyCoverUrl, S.author as studyAuthor
    FROM study_lessons L
    INNER JOIN studies S ON L.studyId = S.id
    WHERE L.isFavorite = 1
"""
    )
    fun getFavoriteStudyLessons(): Flow<List<FavoriteStudyLessonDto>>

    @Query(
        """
    SELECT * FROM study_lessons
    WHERE studyId = :studyId AND remoteId = :lessonId
    LIMIT 1
"""
    )
    fun getStudyLessonByIdsFlow(
        studyId: Int,
        lessonId: Int
    ): Flow<StudyLessonEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStudyIgnore(study: StudyEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStudyLessonIgnore(lesson: StudyLessonEntity): Long

    @Query(
        """
    UPDATE studies
    SET title = :title,
        author = :author,
        image_url = :imageUrl,
        description = :description
    WHERE id = :studyId
"""
    )
    suspend fun updateStudyMetadata(
        studyId: Int,
        title: String,
        author: String,
        imageUrl: String,
        description: String
    )

    @Query(
        """
    UPDATE study_lessons
    SET title = :title,
        url = :url,
        duration = :duration
    WHERE studyId = :studyId AND remoteId = :remoteId
"""
    )
    suspend fun updateStudyLessonMetadata(
        studyId: Int,
        remoteId: Int,
        title: String,
        url: String,
        duration: Long
    )


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(content: MoreContentEntity)

    @Query("SELECT * FROM more_content WHERE id = 1 LIMIT 1")
    fun observe(): Flow<MoreContentEntity?>

    @Query("SELECT * FROM more_content WHERE id = 1 LIMIT 1")
    suspend fun get(): MoreContentEntity?

    @Query("DELETE FROM more_content")
    suspend fun clear()
}