package br.app.ide.ouvindoabiblia.ui.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import br.app.ide.ouvindoabiblia.ui.components.AppAsyncImage
import br.app.ide.ouvindoabiblia.ui.components.RichTextContent
import br.app.ide.ouvindoabiblia.ui.home.components.ErrorScreen
import br.app.ide.ouvindoabiblia.ui.home.components.LoadingScreen
import br.app.ide.ouvindoabiblia.ui.theme.Accent
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray
import br.app.ide.ouvindoabiblia.ui.theme.OuvindoABibliaTheme
import br.app.ide.ouvindoabiblia.ui.theme.RosyBeige
import br.app.ide.ouvindoabiblia.ui.theme.SlateBlue


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    bottomContentPadding: Dp = 0.dp,
    onSectionClick: (String) -> Unit = {},
    viewModel: MoreViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    var selectedMenuItem by remember { mutableStateOf<MoreMenuItemUi?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    when (uiState) {
        MoreUiState.Loading -> LoadingScreen()

        is MoreUiState.Error -> ErrorScreen(uiState.message) {
            viewModel.handle(MoreIntent.Retry)
        }

        is MoreUiState.Success -> {
            MoreContent(
                content = uiState.content,
                bottomContentPadding = bottomContentPadding,
                onSectionClick = { itemId ->
                    val item = moreMenuItems.firstOrNull { it.id == itemId }
                    if (item != null) {
                        selectedMenuItem = item
                    }
                }
            )

            selectedMenuItem?.let { item ->
                val selectedSection = uiState.content.sections.firstOrNull { it.id == item.id }

                ModalBottomSheet(
                    onDismissRequest = { selectedMenuItem = null },
                    sheetState = sheetState,
                    containerColor = CreamBackground
                ) {
                    MoreSectionSheetContent(
                        item = item,
                        section = selectedSection
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreSectionSheetContent(
    item: MoreMenuItemUi,
    section: MoreSectionDto?
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
//                Box(
//                    modifier = Modifier.fillMaxWidth(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .padding(top = 4.dp, bottom = 12.dp)
//                            .size(width = 42.dp, height = 4.dp)
//                            .background(
//                                color = RosyBeige.copy(alpha = 0.8f),
//                                shape = RoundedCornerShape(50)
//                            )
//                    )
//                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlueDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SlateBlue,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                HorizontalDivider(
                    color = RosyBeige.copy(alpha = 0.45f),
                    thickness = 1.dp
                )
            }
        }

        when (section?.type) {
            MoreSectionTypeDto.LONG_TEXT -> {

                item {
                    RichTextContent(
                        text = section.content.text ?: "Sem conteúdo disponível.",
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 28.sp
                        ),
                        textColor = DeepBlueDark,
                        paragraphSpacing = 16.dp,
                        quoteBarColor = Accent.copy(alpha = 0.45f),
                        quoteTextColor = DeepBlueDark,
                        quoteBackgroundColor = Accent.copy(alpha = 0.16f)
                    )
                }

            }

            MoreSectionTypeDto.RIGHTS_LIST -> {
                section.content.description?.takeIf { it.isNotBlank() }?.let { description ->
                    item {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateBlue,
                            lineHeight = 22.sp
                        )
                    }
                }

                itemsIndexed(
                    items = section.content.sources,
                    key = { index, source -> "${source.id}_$index" }
                ) { _, source ->
                    MoreSheetInfoBlock(
                        title = source.name,
                        subtitle = source.role,
                        lines = listOf(
                            source.description,
                            "Licença: ${source.license}",
                            source.contact?.email?.let { "E-mail: $it" },
                            source.contact?.website?.let { "Site: $it" },
                            source.sourceUrl?.let { "Fonte: $it" }
                        ),
                        imageUrl = source.imageUrl
                    )
                }
            }

            MoreSectionTypeDto.ASSET_LIST -> {
                itemsIndexed(
                    items = section.content.assets,
                    key = { index, asset -> "${asset.id}_$index" }
                ) { _, asset ->
                    MoreSheetInfoBlock(
                        title = asset.title,
                        subtitle = asset.author,
                        lines = listOf(
                            "Fonte: ${asset.source}",
                            "Licença: ${asset.license}",
                            asset.notes
                        ),
                        imageUrl = asset.imageUrl
                    )
                }
            }

            MoreSectionTypeDto.PEOPLE_LIST -> {
                itemsIndexed(
                    items = section.content.people,
                    key = { index, person -> "${person.id}_$index" }
                ) { _, person ->
                    MoreSheetInfoBlock(
                        title = person.name,
                        subtitle = person.role,
                        lines = listOf(
                            person.description,
                            person.email?.let { "E-mail: $it" },
                            person.website?.let { "Site: $it" }
                        ),
                        imageUrl = person.imageUrl
                    )
                }
            }

            MoreSectionTypeDto.LIBRARY_LIST -> {
                itemsIndexed(
                    items = section.content.libraries,
                    key = { index, library -> "${library.id}_$index" }
                ) { _, library ->
                    MoreSheetInfoBlock(
                        title = library.name,
                        subtitle = "Versão ${library.version}",
                        lines = listOf(
                            "Licença: ${library.license}",
                            "Site: ${library.website}"
                        )
                    )
                }
            }

            null -> {
                item {
                    Text(
                        text = "Conteúdo não disponível para esta seção.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LavenderGray
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreSheetInfoBlock(
    title: String,
    subtitle: String? = null,
    lines: List<String?>,
    imageUrl: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (!imageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        color = RosyBeige.copy(alpha = 0.18f)
                    )
            ) {
                AppAsyncImage(
                    imageUrl = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DeepBlueDark
            )

            subtitle?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateBlue,
                    fontWeight = FontWeight.Medium
                )
            }

            lines.filterNotNull().filter { it.isNotBlank() }.forEach { line ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LavenderGray
                )
            }
        }
    }
}

@Composable
private fun MoreContent(
    content: MoreContentDto,
    bottomContentPadding: Dp,
    onSectionClick: (String) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val horizontalScreenPadding = 16.dp
    val sectionSpacing = 12.dp

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
            top = 24.dp,
            bottom = 0.dp
        ),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        item {
            MoreHeroSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding + 8.dp),
                appName = "Ouvindo a Bíblia",
                verseText = "\"Bem-aventurado aquele que lê, e os que ouvem as palavras da profecia e guardam as coisas nela escritas...\"",
                verseReference = "Apocalipse 1:3"
            )
        }

        items(
            items = moreMenuItems,
            key = { it.id }
        ) { item ->
            Box(
                modifier = Modifier.padding(horizontal = horizontalScreenPadding)
            ) {
                MoreSectionCard(
                    item = item,
                    onClick = { onSectionClick(item.id) }
                )
            }
        }

        item {
            MoreFooterVersionSection(
                version = content.version,
                lastUpdated = content.lastUpdated,
                bottomInset = resolvedBottomPadding
            )
        }
    }
}

@Composable
private fun MoreHeroSection(
    modifier: Modifier = Modifier,
    appName: String,
    verseText: String,
    verseReference: String
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CreamBackground)
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 20.dp
            )
    ) {
        Text(
            text = appName,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = DeepBlueDark,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = verseText,
            fontStyle = Italic,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            lineHeight = 23.sp,
            color = DeepBlueDark
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = verseReference,
            style = MaterialTheme.typography.bodyMedium,
            color = SlateBlue,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MoreFooterVersionSection(
    version: String,
    lastUpdated: String,
    bottomInset: Dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LavenderGray.copy(alpha = 0.12f))
            .padding(
                top = 14.dp,
                bottom = bottomInset
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 35.dp, end = 16.dp)
        ) {
            Text(
                text = "Versão",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DeepBlueDark
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = version,
                style = MaterialTheme.typography.bodyLarge,
                color = SlateBlue
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "($lastUpdated)",
                style = MaterialTheme.typography.bodySmall,
                color = LavenderGray
            )
        }
    }
}

@Composable
private fun MoreSectionCard(
    item: MoreMenuItemUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .padding(top = 2.dp),
                shape = CircleShape,
                color = Accent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = DeepBlueDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = DeepBlueDark,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LavenderGray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Abrir",
                tint = RosyBeige.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
        }
    }
}

private fun previewMoreContent(): MoreContentDto {
    return MoreContentDto(
        screen = "more",
        lastUpdated = "06/04/2026",
        version = "1.0.0",
        sections = listOf(
            MoreSectionDto(
                id = "about",
                title = "Sobre o app",
                type = MoreSectionTypeDto.LONG_TEXT,
                content = MoreSectionContentDto(
                    text = "O Ouvindo a Bíblia foi criado para oferecer uma experiência reverente, fluida e acessível de escuta bíblica e conteúdos cristãos em áudio."
                )
            ),
            MoreSectionDto(
                id = "mission",
                title = "Missão",
                type = MoreSectionTypeDto.LONG_TEXT,
                content = MoreSectionContentDto(
                    text = "Nossa missão é aproximar pessoas da Palavra de Deus por meio de uma experiência de escuta simples, bela e contínua."
                )
            ),
            MoreSectionDto(
                id = "privacy",
                title = "Privacidade",
                type = MoreSectionTypeDto.LONG_TEXT,
                content = MoreSectionContentDto(
                    text = "Respeitamos a privacidade do usuário e buscamos coletar apenas o mínimo necessário para o funcionamento e melhoria do app."
                )
            ),
            MoreSectionDto(
                id = "bible_audio_rights",
                title = "Direitos dos áudios bíblicos",
                type = MoreSectionTypeDto.RIGHTS_LIST,
                content = MoreSectionContentDto(
                    description = "Narrações bíblicas utilizadas no aplicativo.",
                    sources = listOf(
                        MoreRightsSourceDto(
                            id = "audio_bible_1",
                            name = "João da Silva",
                            role = "Narrador",
                            contentType = MoreRightsContentTypeDto.BIBLE_AUDIO,
                            description = "Narração da Bíblia em áudio.",
                            license = "Uso autorizado",
                            sourceUrl = "https://example.com",
                            contact = MoreContactDto(
                                email = "contato@example.com",
                                website = "https://example.com"
                            ),
                            imageUrl = "https://placehold.co/300x300/png"
                        )
                    )
                )
            ),
            MoreSectionDto(
                id = "study_audio_rights",
                title = "Direitos dos estudos",
                type = MoreSectionTypeDto.RIGHTS_LIST,
                content = MoreSectionContentDto(
                    description = "Estudos e conteúdos em áudio utilizados no aplicativo.",
                    sources = listOf(
                        MoreRightsSourceDto(
                            id = "audio_study_1",
                            name = "Maria Oliveira",
                            role = "Autora",
                            contentType = MoreRightsContentTypeDto.STUDY_AUDIO,
                            description = "Conteúdo de estudo adaptado para áudio.",
                            license = "Uso autorizado",
                            sourceUrl = "https://example.com/estudos",
                            contact = MoreContactDto(
                                email = "maria@example.com",
                                website = "https://example.com/estudos"
                            ),
                            imageUrl = null
                        )
                    )
                )
            ),
            MoreSectionDto(
                id = "cover_rights",
                title = "Capas e imagens",
                type = MoreSectionTypeDto.ASSET_LIST,
                content = MoreSectionContentDto(
                    assets = listOf(
                        MoreAssetDto(
                            id = "asset_1",
                            title = "Capa de Romanos",
                            author = "Equipe Editorial",
                            source = "Acervo interno",
                            license = "Uso editorial",
                            notes = "Imagem usada como referência visual.",
                            imageUrl = null
                        ),
                        MoreAssetDto(
                            id = "asset_2",
                            title = "Capa de Colossenses",
                            author = "Equipe Editorial",
                            source = "Acervo interno",
                            license = "Uso editorial",
                            notes = null,
                            imageUrl = null
                        )
                    )
                )
            ),
            MoreSectionDto(
                id = "curation",
                title = "Curadoria",
                type = MoreSectionTypeDto.PEOPLE_LIST,
                content = MoreSectionContentDto(
                    people = listOf(
                        MorePersonDto(
                            id = "person_1",
                            name = "Maria Oliveira",
                            role = "Curadoria de conteúdo",
                            description = "Responsável pela organização e revisão de parte do conteúdo publicado.",
                            website = null,
                            email = "maria@example.com",
                            imageUrl = null
                        ),
                        MorePersonDto(
                            id = "person_2",
                            name = "Pedro Santos",
                            role = "Revisão teológica",
                            description = "Colaborador na revisão de estudos e materiais cristãos.",
                            website = null,
                            email = null,
                            imageUrl = null
                        )
                    )
                )
            ),
            MoreSectionDto(
                id = "licenses",
                title = "Licenças",
                type = MoreSectionTypeDto.LIBRARY_LIST,
                content = MoreSectionContentDto(
                    libraries = listOf(
                        MoreLibraryDto(
                            id = "lib_1",
                            name = "Jetpack Compose",
                            version = "2024.12.01",
                            license = "Apache-2.0",
                            website = "https://developer.android.com/jetpack/compose"
                        ),
                        MoreLibraryDto(
                            id = "lib_2",
                            name = "Media3",
                            version = "1.9.2",
                            license = "Apache-2.0",
                            website = "https://developer.android.com/media"
                        ),
                        MoreLibraryDto(
                            id = "lib-hilt",
                            name = "Hilt",
                            version = "2.51.1",
                            license = "Apache-2.0",
                            website = "https://dagger.dev/hilt/"
                        ),
                        MoreLibraryDto(
                            id = "lib-hilt",
                            name = "Hilt Duplicate Demo",
                            version = "2.51.1",
                            license = "Apache-2.0",
                            website = "https://dagger.dev/hilt/"
                        )
                    )
                )
            )
        )
    )
}

@Preview(
    name = "More screen",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412,
    heightDp = 915
)
@Composable
private fun PreviewMoreContent() {
    OuvindoABibliaTheme {
        MoreContent(
            content = previewMoreContent(),
            bottomContentPadding = 72.dp,
            onSectionClick = {}
        )
    }
}

@Preview(
    name = "More section card",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 360
)
@Composable
private fun PreviewMoreSectionCard() {
    OuvindoABibliaTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CreamBackground)
                .padding(20.dp)
        ) {
            MoreSectionCard(
                item = moreMenuItems.first(),
                onClick = {}
            )
        }
    }
}

@Preview(
    name = "More footer version",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 360
)
@Composable
private fun PreviewMoreFooterVersionSection() {
    OuvindoABibliaTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CreamBackground)
        ) {
            MoreFooterVersionSection(
                version = "1.0.0",
                lastUpdated = "06/04/2026",
                bottomInset = 0.dp
            )
        }
    }
}

@Preview(
    name = "More sheet - long text",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412,
    heightDp = 800
)
@Composable
private fun PreviewMoreSectionSheetLongText() {
    OuvindoABibliaTheme {
        MoreSectionSheetContent(
            item = moreMenuItems.first { it.id == "about" },
            section = previewMoreContent().sections.first { it.id == "about" }
        )
    }
}

@Preview(
    name = "More sheet - rights list",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412,
    heightDp = 800
)
@Composable
private fun PreviewMoreSectionSheetRightsList() {
    OuvindoABibliaTheme {
        MoreSectionSheetContent(
            item = moreMenuItems.first { it.id == "bible_audio_rights" },
            section = previewMoreContent().sections.first { it.id == "bible_audio_rights" }
        )
    }
}

@Preview(
    name = "More sheet - asset list",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412,
    heightDp = 800
)
@Composable
private fun PreviewMoreSectionSheetAssetList() {
    OuvindoABibliaTheme {
        MoreSectionSheetContent(
            item = moreMenuItems.first { it.id == "cover_rights" },
            section = previewMoreContent().sections.first { it.id == "cover_rights" }
        )
    }
}

@Preview(
    name = "More sheet - people list",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412,
    heightDp = 800
)
@Composable
private fun PreviewMoreSectionSheetPeopleList() {
    OuvindoABibliaTheme {
        MoreSectionSheetContent(
            item = moreMenuItems.first { it.id == "curation" },
            section = previewMoreContent().sections.first { it.id == "curation" }
        )
    }
}

@Preview(
    name = "More sheet - library list",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412,
    heightDp = 800
)
@Composable
private fun PreviewMoreSectionSheetLibraryList() {
    OuvindoABibliaTheme {
        MoreSectionSheetContent(
            item = moreMenuItems.first { it.id == "licenses" },
            section = previewMoreContent().sections.first { it.id == "licenses" }
        )
    }
}

@Preview(
    name = "More sheet - no content",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412,
    heightDp = 800
)
@Composable
private fun PreviewMoreSectionSheetNoContent() {
    OuvindoABibliaTheme {
        MoreSectionSheetContent(
            item = moreMenuItems.first(),
            section = null
        )
    }
}