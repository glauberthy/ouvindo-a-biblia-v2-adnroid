package br.app.ide.ouvindoabiblia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import br.app.ide.ouvindoabiblia.data.local.entity.BookEntity
import br.app.ide.ouvindoabiblia.data.local.entity.ChapterEntity
import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo
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

    // Uma transação garante que limpa e insere tudo de uma vez com segurança
    @Transaction
    suspend fun refreshBibleData(books: List<BookEntity>, chapters: List<ChapterEntity>) {
        clearBooks()
        clearChapters()
        insertBooks(books)
        insertChapters(chapters)
    }
}