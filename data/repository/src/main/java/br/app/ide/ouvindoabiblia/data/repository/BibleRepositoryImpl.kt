package br.app.ide.ouvindoabiblia.data.repository

import br.app.ide.ouvindoabiblia.data.local.dao.BibleDao
import br.app.ide.ouvindoabiblia.data.local.entity.BookEntity
import br.app.ide.ouvindoabiblia.data.local.entity.ChapterEntity
import br.app.ide.ouvindoabiblia.data.remote.api.BibleApi
import br.app.ide.ouvindoabiblia.data.remote.dto.BookDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BibleRepositoryImpl @Inject constructor(
    private val api: BibleApi,
    private val dao: BibleDao
) : BibleRepository {

    override fun getBooks(): Flow<List<BookEntity>> = dao.getAllBooks()

    override fun getChapters(bookId: String): Flow<List<ChapterEntity>> = dao.getChaptersForBook(bookId)

    override suspend fun getBook(bookId: String): BookEntity? = dao.getBookById(bookId)

    override suspend fun syncBibleData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Baixar o JSON gigante
            val response = api.getBibleIndex()

            // 2. Preparar listas para inserção em massa
            val booksToInsert = mutableListOf<BookEntity>()
            val chaptersToInsert = mutableListOf<ChapterEntity>()

            // 3. Função auxiliar para mapear (Evita código duplicado para AT e NT)
            fun mapTestament(testamentCode: String, booksMap: Map<String, BookDto>) {
                booksMap.forEach { (slug, dto) ->
                    // Cria Entidade do Livro
                    booksToInsert.add(
                        BookEntity(
                            bookId = slug, // ex: "genesis"
                            name = dto.nome,
                            testament = testamentCode, // "at" ou "nt"
                            folderPath = dto.path,
                            imageUrl = dto.imageUrl,
                            totalChapters = dto.totalChapters
                        )
                    )

                    // Cria Entidades dos Capítulos desse Livro
                    dto.capitulos.forEach { chapterDto ->
                        chaptersToInsert.add(
                            ChapterEntity(
                                bookId = slug, // Chave Estrangeira
                                number = chapterDto.numero,
                                audioUrl = chapterDto.url,
                                filename = chapterDto.arquivo
                            )
                        )
                    }
                }
            }

            // 4. Processar Antigo e Novo Testamento
            mapTestament("at", response.testamentos.antigoTestamento)
            mapTestament("nt", response.testamentos.novoTestamento)

            // 5. Salvar no Banco (Transação atômica: apaga tudo e insere o novo)
            if (booksToInsert.isNotEmpty()) {
                dao.refreshBibleData(booksToInsert, chaptersToInsert)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}