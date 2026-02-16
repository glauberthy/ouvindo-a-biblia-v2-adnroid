package br.app.ide.ouvindoabiblia.data.remote.dto

data class ThemesResponse(
    val meta: ThemeMetaDto,
    val themes: List<ThemeDto>
)

data class ThemeMetaDto(val version: String)

data class ThemeDto(
    val id: Int,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val moments: List<MomentDto>
)

data class MomentDto(
    val title: String,
    val bookId: Int,
    val chapter: Int,
    val startMs: Long,
    val endMs: Long,
    val reference: String
)