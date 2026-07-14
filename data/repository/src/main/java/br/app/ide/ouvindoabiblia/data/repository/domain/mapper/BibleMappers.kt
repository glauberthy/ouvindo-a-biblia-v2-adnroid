package br.app.ide.ouvindoabiblia.data.repository.domain.mapper

import br.app.ide.ouvindoabiblia.data.local.entity.BookEntity
import br.app.ide.ouvindoabiblia.data.local.entity.ChapterEntity
import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Book
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Chapter

internal fun BookEntity.toDomain(): Book = Book(
    numericId = numericId,
    bookId = bookId,
    name = name,
    testament = testament,
    folderPath = folderPath,
    imageUrl = imageUrl,
    totalChapters = totalChapters
)

internal fun ChapterWithBookInfo.toDomain(): Chapter = Chapter(
    id = chapter.id,
    bookId = chapter.bookId,
    number = chapter.number,
    audioUrl = chapter.audioUrl,
    isFavorite = chapter.isFavorite,
    bookName = bookName,
    coverUrl = coverUrl,
    testament = testament,
    totalChapters = totalChapters
)

/** Capítulo "cru" (sem JOIN de livro): campos de livro ficam nos defaults do domínio. */
internal fun ChapterEntity.toDomain(): Chapter = Chapter(
    id = id,
    bookId = bookId,
    number = number,
    audioUrl = audioUrl,
    isFavorite = isFavorite
)
