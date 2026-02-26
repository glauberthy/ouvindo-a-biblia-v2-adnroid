package br.app.ide.ouvindoabiblia.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThemesResponse(
    val meta: ThemeMetaDto,
    val themes: List<ThemeDto>
)

@Serializable
data class ThemeMetaDto(val version: String)

@Serializable
data class ThemeDto(
    val id: Int,
    val title: String,
    val description: String,
    // SINALIZAÇÃO: Mapeia o snake_case do JSON para o camelCase do Kotlin
    @SerialName("image_url") val imageUrl: String?,
    val moments: List<MomentDto>
)

@Serializable
data class MomentDto(
    val title: String,
    @SerialName("book_id") val bookId: Int,
    val chapter: Int,
    @SerialName("start_ms") val startMs: Long,
    @SerialName("end_ms") val endMs: Long,
    val reference: String
)