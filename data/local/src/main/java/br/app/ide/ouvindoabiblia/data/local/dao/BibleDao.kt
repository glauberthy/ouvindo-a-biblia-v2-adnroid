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
            books.image_url as coverUrl 
        FROM chapters 
        INNER JOIN books ON chapters.book_id = books.book_id 
        WHERE chapters.book_id = :bookId
        ORDER BY chapters.chapter_number ASC
    """
    )
    fun getChaptersWithBookInfo(bookId: String): Flow<List<ChapterWithBookInfo>>

    // Busca capítulos de um livro específico
    @Query("SELECT * FROM chapters WHERE book_id = :bookId ORDER BY chapter_number ASC")
    fun getChaptersForBook(bookId: String): Flow<List<ChapterEntity>>

    // Busca um único livro (útil para pegar a capa no player)
    @Query("SELECT * FROM books WHERE book_id = :bookId LIMIT 1")
    suspend fun getBookById(bookId: String): BookEntity?

    // --- ESCRITA (Usada quando o app inicia e baixa o JSON) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

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
        clearBooks()
        clearChapters()
        insertBooks(books)
        insertChapters(chapters)
    }

    @Query("UPDATE chapters SET is_favorite = :isFavorite WHERE id = :chapterId")
    suspend fun updateFavoriteStatus(chapterId: Long, isFavorite: Boolean)


    // Busca um capítulo específico pelo ID (Mantido para compatibilidade, se usado em outro lugar)
    @Transaction
    @Query(
        """
        SELECT 
            chapters.*, 
            books.name as bookName, 
            books.image_url as coverUrl 
        FROM chapters 
        INNER JOIN books ON chapters.book_id = books.book_id 
        WHERE chapters.audio_url = :audioUrl OR chapters.book_id = :audioUrl 
        LIMIT 1
    """
    )
    suspend fun getChapterWithBookInfoById(audioUrl: String): ChapterWithBookInfo?

    // -------------------------------------------------------------------------
    // --- NOVO: MÉTODOS PARA O PLAYBACK STATE (Substituindo DataStore) ---
    // -------------------------------------------------------------------------

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
        INNER JOIN books B ON C.book_id = B.book_id
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
            books.image_url as coverUrl 
        FROM chapters 
        INNER JOIN books ON chapters.book_id = books.book_id 
        WHERE chapters.is_favorite = 1
        ORDER BY books.name ASC, chapters.chapter_number ASC
    """
    )
    fun getFavoriteChapters(): Flow<List<ChapterWithBookInfo>>

    // No BibleDao.kt

    @Query("SELECT * FROM chapters WHERE id = :chapterId LIMIT 1")
    fun getChapterByIdFlow(chapterId: Long): Flow<ChapterEntity?>
}