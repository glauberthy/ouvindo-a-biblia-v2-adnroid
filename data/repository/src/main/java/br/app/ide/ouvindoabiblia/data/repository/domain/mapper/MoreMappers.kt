package br.app.ide.ouvindoabiblia.data.repository.domain.mapper

import br.app.ide.ouvindoabiblia.data.remote.dto.MoreAssetDto
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreContactDto
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreContentDto
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreLibraryDto
import br.app.ide.ouvindoabiblia.data.remote.dto.MorePersonDto
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreRightsContentTypeDto
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreRightsSourceDto
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreSectionContentDto
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreSectionDto
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreSectionTypeDto
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Asset
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Contact
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Library
import br.app.ide.ouvindoabiblia.data.repository.domain.model.MoreContent
import br.app.ide.ouvindoabiblia.data.repository.domain.model.MoreSection
import br.app.ide.ouvindoabiblia.data.repository.domain.model.MoreSectionContent
import br.app.ide.ouvindoabiblia.data.repository.domain.model.MoreSectionType
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Person
import br.app.ide.ouvindoabiblia.data.repository.domain.model.RightsContentType
import br.app.ide.ouvindoabiblia.data.repository.domain.model.RightsSource

internal fun MoreContentDto.toDomain(): MoreContent = MoreContent(
    screen = screen,
    lastUpdated = lastUpdated,
    version = version,
    sections = sections.map { it.toDomain() }
)

internal fun MoreSectionDto.toDomain(): MoreSection = MoreSection(
    id = id,
    title = title,
    type = type.toDomain(),
    content = content.toDomain()
)

internal fun MoreSectionTypeDto.toDomain(): MoreSectionType = when (this) {
    MoreSectionTypeDto.LONG_TEXT -> MoreSectionType.LONG_TEXT
    MoreSectionTypeDto.RIGHTS_LIST -> MoreSectionType.RIGHTS_LIST
    MoreSectionTypeDto.ASSET_LIST -> MoreSectionType.ASSET_LIST
    MoreSectionTypeDto.PEOPLE_LIST -> MoreSectionType.PEOPLE_LIST
    MoreSectionTypeDto.LIBRARY_LIST -> MoreSectionType.LIBRARY_LIST
}

internal fun MoreSectionContentDto.toDomain(): MoreSectionContent = MoreSectionContent(
    text = text,
    description = description,
    sources = sources.map { it.toDomain() },
    assets = assets.map { it.toDomain() },
    people = people.map { it.toDomain() },
    libraries = libraries.map { it.toDomain() }
)

internal fun MoreRightsSourceDto.toDomain(): RightsSource = RightsSource(
    id = id,
    name = name,
    role = role,
    contentType = contentType.toDomain(),
    description = description,
    license = license,
    sourceUrl = sourceUrl,
    contact = contact?.toDomain(),
    imageUrl = imageUrl
)

internal fun MoreRightsContentTypeDto.toDomain(): RightsContentType = when (this) {
    MoreRightsContentTypeDto.BIBLE_AUDIO -> RightsContentType.BIBLE_AUDIO
    MoreRightsContentTypeDto.STUDY_AUDIO -> RightsContentType.STUDY_AUDIO
}

internal fun MoreContactDto.toDomain(): Contact = Contact(email = email, website = website)

internal fun MoreAssetDto.toDomain(): Asset = Asset(
    id = id,
    title = title,
    author = author,
    source = source,
    license = license,
    notes = notes,
    imageUrl = imageUrl
)

internal fun MorePersonDto.toDomain(): Person = Person(
    id = id,
    name = name,
    role = role,
    description = description,
    website = website,
    email = email,
    imageUrl = imageUrl
)

internal fun MoreLibraryDto.toDomain(): Library = Library(
    id = id,
    name = name,
    version = version,
    license = license,
    website = website
)
