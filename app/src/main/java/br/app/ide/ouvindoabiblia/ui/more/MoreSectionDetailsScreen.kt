package br.app.ide.ouvindoabiblia.ui.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Asset
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Library
import br.app.ide.ouvindoabiblia.data.repository.domain.model.MoreSection
import br.app.ide.ouvindoabiblia.data.repository.domain.model.MoreSectionType
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Person
import br.app.ide.ouvindoabiblia.data.repository.domain.model.RightsSource
import br.app.ide.ouvindoabiblia.ui.components.AppAsyncImage
import br.app.ide.ouvindoabiblia.ui.theme.Accent
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray
import br.app.ide.ouvindoabiblia.ui.theme.RosyBeige
import br.app.ide.ouvindoabiblia.ui.theme.SlateBlue

@Composable
fun MoreSectionDetailsScreen(
    section: MoreSection,
    bottomContentPadding: Dp = 0.dp,
    onBackClick: () -> Unit,
    onUrlClick: (String) -> Unit = {}
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val resolvedBottomPadding = if (bottomContentPadding == 0.dp) {
        navBarPadding + 16.dp
    } else {
        bottomContentPadding
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = statusBarPadding + 16.dp,
            bottom = resolvedBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MoreSectionHeader(
                title = section.title,
                icon = section.icon(),
                onBackClick = onBackClick
            )
        }

        when (section.type) {
            MoreSectionType.LONG_TEXT -> {
                item {
                    InfoCard {
                        Text(
                            text = section.content.text.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = DeepBlueDark,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                        )
                    }
                }
            }

            MoreSectionType.RIGHTS_LIST -> {
                section.content.description?.takeIf { it.isNotBlank() }?.let { description ->
                    item {
                        InfoCard {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = LavenderGray
                            )
                        }
                    }
                }

                items(
                    items = section.content.sources,
                    key = { it.id }
                ) { source ->
                    RightsSourceCard(
                        source = source,
                        onUrlClick = onUrlClick
                    )
                }
            }

            MoreSectionType.ASSET_LIST -> {
                items(
                    items = section.content.assets,
                    key = { it.id }
                ) { asset ->
                    AssetCard(
                        asset = asset,
                        onUrlClick = onUrlClick
                    )
                }
            }

            MoreSectionType.PEOPLE_LIST -> {
                items(
                    items = section.content.people,
                    key = { it.id }
                ) { person ->
                    PersonCard(
                        person = person,
                        onUrlClick = onUrlClick
                    )
                }
            }

            MoreSectionType.LIBRARY_LIST -> {
                items(
                    items = section.content.libraries,
                    key = { it.id }
                ) { library ->
                    LibraryCard(
                        library = library,
                        onUrlClick = onUrlClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreSectionHeader(
    title: String,
    icon: ImageVector,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = CreamBackground.copy(alpha = 0.92f),
            tonalElevation = 0.dp,
            shadowElevation = 2.dp
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = DeepBlueDark
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = Accent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = DeepBlueDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = DeepBlueDark,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InfoCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFCFA)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = RosyBeige.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun RightsSourceCard(
    source: RightsSource,
    onUrlClick: (String) -> Unit
) {
    InfoCard {
        PersonLikeHeader(
            title = source.name,
            subtitle = source.role,
            imageUrl = source.imageUrl,
            placeholderIcon = Icons.Filled.GraphicEq
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = source.description,
            style = MaterialTheme.typography.bodyMedium,
            color = DeepBlueDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        MetaLine(label = "Licença", value = source.license)

        source.sourceUrl?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(8.dp))
            LinkLine(label = "Fonte", value = it, onClick = { onUrlClick(it) })
        }

        source.contact?.website?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(8.dp))
            LinkLine(label = "Website", value = it, onClick = { onUrlClick(it) })
        }

        source.contact?.email?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(8.dp))
            MetaLine(label = "E-mail", value = it)
        }
    }
}

@Composable
private fun AssetCard(
    asset: Asset,
    onUrlClick: (String) -> Unit
) {
    InfoCard {
        asset.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
            AppAsyncImage(
                imageUrl = imageUrl,
                contentDescription = asset.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = asset.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DeepBlueDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        MetaLine(label = "Autor/Fonte", value = asset.author)
        Spacer(modifier = Modifier.height(8.dp))
        MetaLine(label = "Licença", value = asset.license)

        asset.notes?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = LavenderGray
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        LinkLine(label = "Origem", value = asset.source, onClick = { onUrlClick(asset.source) })
    }
}

@Composable
private fun PersonCard(
    person: Person,
    onUrlClick: (String) -> Unit
) {
    InfoCard {
        PersonLikeHeader(
            title = person.name,
            subtitle = person.role,
            imageUrl = person.imageUrl,
            placeholderIcon = Icons.Filled.MenuBook
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = person.description,
            style = MaterialTheme.typography.bodyMedium,
            color = DeepBlueDark
        )

        person.website?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(12.dp))
            LinkLine(label = "Website", value = it, onClick = { onUrlClick(it) })
        }

        person.email?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(8.dp))
            MetaLine(label = "E-mail", value = it)
        }
    }
}

@Composable
private fun LibraryCard(
    library: Library,
    onUrlClick: (String) -> Unit
) {
    InfoCard {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Accent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = null,
                        tint = DeepBlueDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = library.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlueDark
                )
                Text(
                    text = "Versão ${library.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        MetaLine(label = "Licença", value = library.license)
        Spacer(modifier = Modifier.height(8.dp))
        LinkLine(
            label = "Website",
            value = library.website,
            onClick = { onUrlClick(library.website) })
    }
}

@Composable
private fun PersonLikeHeader(
    title: String,
    subtitle: String,
    imageUrl: String?,
    placeholderIcon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!imageUrl.isNullOrBlank()) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = Accent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                AppAsyncImage(
                    imageUrl = imageUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = Accent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = placeholderIcon,
                        contentDescription = null,
                        tint = DeepBlueDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DeepBlueDark
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SlateBlue
            )
        }
    }
}

@Composable
private fun MetaLine(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = SlateBlue,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = DeepBlueDark
        )
    }
}

@Composable
private fun LinkLine(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = SlateBlue,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Link,
                contentDescription = null,
                tint = RosyBeige,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = DeepBlueDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun MoreSection.icon(): ImageVector {
    return when (type) {
        MoreSectionType.LONG_TEXT -> when (id) {
            "privacy" -> Icons.Filled.PrivacyTip
            "mission" -> Icons.AutoMirrored.Filled.MenuBook
            else -> Icons.Filled.Info
        }

        MoreSectionType.RIGHTS_LIST -> Icons.Filled.GraphicEq
        MoreSectionType.ASSET_LIST -> Icons.Filled.Image
        MoreSectionType.PEOPLE_LIST -> Icons.AutoMirrored.Filled.MenuBook
        MoreSectionType.LIBRARY_LIST -> Icons.Filled.Code
    }
}