package br.app.ide.ouvindoabiblia.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleResponseDto(
    @SerialName("meta")
    val meta: MetaDto,

    @SerialName("testamentos")
    val testamentos: TestamentsDto
)

@Serializable
data class MetaDto(
    val version: String,
    val description: String
)

@Serializable
data class TestamentsDto(
    // Truque de Mestre: Usamos Map para ler chaves dinâmicas ("genesis": {}, "exodo": {})
    @SerialName("at")
    val antigoTestamento: Map<String, BookDto>,

    @SerialName("nt")
    val novoTestamento: Map<String, BookDto>
)

@Serializable
data class BookDto(
    @SerialName("nome_formatado")
    val nome: String,

    @SerialName("path")
    val path: String,

    @SerialName("imagem_url")
    val imageUrl: String? = null, // Pode vir nulo

    @SerialName("total_capitulos")
    val totalChapters: Int,

    @SerialName("capitulos")
    val capitulos: List<ChapterDto>
)

@Serializable
data class ChapterDto(
    @SerialName("capitulo")
    val numero: Int,

    @SerialName("arquivo")
    val arquivo: String,

    @SerialName("url")
    val url: String
)