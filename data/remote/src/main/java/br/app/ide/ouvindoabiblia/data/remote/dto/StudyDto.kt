package br.app.ide.ouvindoabiblia.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StudyResponseDto(
    val meta: MetaStdDto,
    val estudos: List<StudyDto>
)

@Serializable
data class MetaStdDto(
    val version: String
)

@Serializable
data class StudyDto(
    val id: Int,
    val title: String,
    val author: String? = null, // Pode extrair do seu JSON se adicionar
    val description: String,
    @SerialName("image_url") val imageUrl: String,
    val audios: List<StudyAudioDto>
)

@Serializable
data class StudyAudioDto(
    val id: Int,
    val title: String,
    val url: String,
    val duration: Long? = null, // Opcional
    val description: String? = null // Opcional: JSON antigo/aulas sem o campo seguem parseando
)