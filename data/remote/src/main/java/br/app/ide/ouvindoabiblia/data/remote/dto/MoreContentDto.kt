package br.app.ide.ouvindoabiblia.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MoreContentDto(
    val screen: String,
    val lastUpdated: String,
    val version: String,
    val sections: List<MoreSectionDto> = emptyList()
)

@Serializable
data class MoreSectionDto(
    val id: String,
    val title: String,
    val type: MoreSectionTypeDto,
    val content: MoreSectionContentDto
)

@Serializable
enum class MoreSectionTypeDto {
    @SerialName("long_text")
    LONG_TEXT,

    @SerialName("rights_list")
    RIGHTS_LIST,

    @SerialName("asset_list")
    ASSET_LIST,

    @SerialName("people_list")
    PEOPLE_LIST,

    @SerialName("library_list")
    LIBRARY_LIST
}

@Serializable
data class MoreSectionContentDto(
    val text: String? = null,
    val description: String? = null,
    val sources: List<MoreRightsSourceDto> = emptyList(),
    val assets: List<MoreAssetDto> = emptyList(),
    val people: List<MorePersonDto> = emptyList(),
    val libraries: List<MoreLibraryDto> = emptyList()
)

@Serializable
data class MoreRightsSourceDto(
    val id: String,
    val name: String,
    val role: String,
    val contentType: MoreRightsContentTypeDto,
    val description: String,
    val license: String,
    val sourceUrl: String? = null,
    val contact: MoreContactDto? = null,
    val imageUrl: String? = null
)

@Serializable
enum class MoreRightsContentTypeDto {
    @SerialName("bible_audio")
    BIBLE_AUDIO,

    @SerialName("study_audio")
    STUDY_AUDIO
}

@Serializable
data class MoreContactDto(
    val email: String? = null,
    val website: String? = null
)

@Serializable
data class MoreAssetDto(
    val id: String,
    val title: String,
    val author: String,
    val source: String,
    val license: String,
    val notes: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class MorePersonDto(
    val id: String,
    val name: String,
    val role: String,
    val description: String,
    val website: String? = null,
    val email: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class MoreLibraryDto(
    val id: String,
    val name: String,
    val version: String,
    val license: String,
    val website: String
)