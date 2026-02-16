package br.app.ide.ouvindoabiblia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import br.app.ide.ouvindoabiblia.data.local.entity.BookEntity
import br.app.ide.ouvindoabiblia.data.local.entity.ChapterEntity
import br.app.ide.ouvindoabiblia.data.local.entity.PlaybackStateEntity
import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo
import br.app.ide.ouvindoabiblia.data.local.model.PlaybackStateDto
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

    // 3. A Query Mestra: Busca o estado + dados do Capítulo + dados do Livro
    // Se o capítulo ou livro não existirem mais, isso não retorna nada (evitando erro!)
    @Transaction
    @Query(
        """
    SELECT 
        P.chapterId, 
        P.positionMs, 
        C.audio_url as audioUrl,
        C.chapter_number as chapterNumber,
        C.book_id as bookId,
        B.name as bookName,
        B.image_url as coverUrl
    FROM playback_state P
    INNER JOIN chapters C ON P.chapterId = C.id
    INNER JOIN books B ON C.book_id = B.numericId 
    WHERE P.id = 1
"""
    )
    fun getLastPlaybackState(): Flow<PlaybackStateDto?>


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
}