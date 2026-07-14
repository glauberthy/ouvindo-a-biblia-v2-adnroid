package br.app.ide.ouvindoabiblia.data.repository.domain.model

/** Conteúdo da aba "Mais" (modelo de domínio; substitui a árvore MoreContentDto na UI). */
data class MoreContent(
    val screen: String,
    val lastUpdated: String,
    val version: String,
    val sections: List<MoreSection>
)

data class MoreSection(
    val id: String,
    val title: String,
    val type: MoreSectionType,
    val content: MoreSectionContent
)

enum class MoreSectionType {
    LONG_TEXT,
    RIGHTS_LIST,
    ASSET_LIST,
    PEOPLE_LIST,
    LIBRARY_LIST
}

data class MoreSectionContent(
    val text: String? = null,
    val description: String? = null,
    val sources: List<RightsSource> = emptyList(),
    val assets: List<Asset> = emptyList(),
    val people: List<Person> = emptyList(),
    val libraries: List<Library> = emptyList()
)

data class RightsSource(
    val id: String,
    val name: String,
    val role: String,
    val contentType: RightsContentType,
    val description: String,
    val license: String,
    val sourceUrl: String? = null,
    val contact: Contact? = null,
    val imageUrl: String? = null
)

enum class RightsContentType {
    BIBLE_AUDIO,
    STUDY_AUDIO
}

data class Contact(
    val email: String? = null,
    val website: String? = null
)

data class Asset(
    val id: String,
    val title: String,
    val author: String,
    val source: String,
    val license: String,
    val notes: String? = null,
    val imageUrl: String? = null
)

data class Person(
    val id: String,
    val name: String,
    val role: String,
    val description: String,
    val website: String? = null,
    val email: String? = null,
    val imageUrl: String? = null
)

data class Library(
    val id: String,
    val name: String,
    val version: String,
    val license: String,
    val website: String
)
