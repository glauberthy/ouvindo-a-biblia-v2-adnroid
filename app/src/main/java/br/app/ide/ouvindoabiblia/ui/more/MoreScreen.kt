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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import br.app.ide.ouvindoabiblia.ui.home.components.ErrorScreen
import br.app.ide.ouvindoabiblia.ui.home.components.LoadingScreen
import br.app.ide.ouvindoabiblia.ui.theme.Accent
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray
import br.app.ide.ouvindoabiblia.ui.theme.OuvindoABibliaTheme
import br.app.ide.ouvindoabiblia.ui.theme.RosyBeige
import br.app.ide.ouvindoabiblia.ui.theme.SlateBlue

@Composable
fun MoreScreen(
    bottomContentPadding: Dp = 0.dp,
    onSectionClick: (String) -> Unit = {},
    viewModel: MoreViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    when (uiState) {
        MoreUiState.Loading -> LoadingScreen()

        is MoreUiState.Error -> ErrorScreen(uiState.message) {
            viewModel.sync()
        }

        is MoreUiState.Success -> {
            MoreContent(
                content = uiState.content,
                bottomContentPadding = bottomContentPadding,
                onSectionClick = onSectionClick
            )
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


    val visibleSections = content.sections.filterNot { it.id == "about" }

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
                verseText = "\"Lâmpada para os meus pés é a tua palavra e, luz para o meu caminho.\"",
                verseReference = "Salmos 119:105"
            )
        }

        items(
            items = visibleSections,
            key = { it.id }
        ) { section ->
            Box(
                modifier = Modifier.padding(horizontal = horizontalScreenPadding)
            ) {
                MoreSectionCard(
                    section = section,
                    onClick = { onSectionClick(section.id) }
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
            modifier = Modifier.fillMaxWidth(),
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
private fun MoreVersionCard(
    version: String,
    lastUpdated: String
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
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Versão do app",
                style = MaterialTheme.typography.titleSmall,
                color = SlateBlue,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = version,
                style = MaterialTheme.typography.titleLarge,
                color = DeepBlueDark,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Conteúdo atualizado em $lastUpdated",
                style = MaterialTheme.typography.bodySmall,
                color = LavenderGray
            )
        }
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
    section: MoreSectionDto,
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
            verticalAlignment = Alignment.CenterVertically
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
                        imageVector = section.icon(),
                        contentDescription = null,
                        tint = DeepBlueDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = DeepBlueDark,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = section.previewText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = LavenderGray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Abrir",
                tint = RosyBeige.copy(alpha = 0.9f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun MoreSectionDto.icon(): ImageVector {
    return when (type) {
        MoreSectionTypeDto.LONG_TEXT -> when (id) {
            "privacy" -> Icons.Filled.PrivacyTip
            "mission" -> Icons.AutoMirrored.Filled.MenuBook
            else -> Icons.Filled.Info
        }

        MoreSectionTypeDto.RIGHTS_LIST -> Icons.Filled.GraphicEq
        MoreSectionTypeDto.ASSET_LIST -> Icons.Filled.Image
        MoreSectionTypeDto.PEOPLE_LIST -> Icons.AutoMirrored.Filled.MenuBook
        MoreSectionTypeDto.LIBRARY_LIST -> Icons.Filled.Code
    }
}

private fun MoreSectionDto.previewText(): String {
    return when (type) {
        MoreSectionTypeDto.LONG_TEXT -> {
            content.text
                ?.replace("\n", " ")
                ?.trim()
                ?.take(110)
                ?.let { if (it.length >= 110) "$it..." else it }
                ?: "Sem conteúdo disponível."
        }

        MoreSectionTypeDto.RIGHTS_LIST -> {
            content.description
                ?: "${content.sources.size} item(ns)"
        }

        MoreSectionTypeDto.ASSET_LIST -> {
            val count = content.assets.size
            if (count == 1) "1 capa ou imagem cadastrada"
            else "$count capas ou imagens cadastradas"
        }

        MoreSectionTypeDto.PEOPLE_LIST -> {
            val count = content.people.size
            if (count == 1) "1 colaborador cadastrado"
            else "$count colaboradores cadastrados"
        }

        MoreSectionTypeDto.LIBRARY_LIST -> {
            val count = content.libraries.size
            if (count == 1) "1 biblioteca listada"
            else "$count bibliotecas listadas"
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
                id = "audio_rights",
                title = "Direitos dos áudios",
                type = MoreSectionTypeDto.RIGHTS_LIST,
                content = MoreSectionContentDto(
                    description = "Narrações e conteúdos em áudio utilizados no aplicativo.",
                    sources = listOf(
                        MoreRightsSourceDto(
                            id = "audio_1",
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
                section = previewMoreContent().sections.first(),
                onClick = {}
            )
        }
    }
}

@Preview(
    name = "More version card",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 360
)
@Preview(
    name = "More version card",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 360
)
@Composable
private fun PreviewMoreVersionCard() {
    OuvindoABibliaTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CreamBackground)
                .padding(20.dp)
        ) {
            MoreVersionCard(
                version = "1.0.0",
                lastUpdated = "06/04/2026"
            )
        }
    }
}