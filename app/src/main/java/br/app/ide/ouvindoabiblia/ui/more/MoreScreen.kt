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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreContentDto
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreSectionDto
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreSectionTypeDto
import br.app.ide.ouvindoabiblia.ui.home.components.ErrorScreen
import br.app.ide.ouvindoabiblia.ui.home.components.LoadingScreen
import br.app.ide.ouvindoabiblia.ui.theme.Accent
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray
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
            top = statusBarPadding + 24.dp,
            bottom = resolvedBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Mais",
                    style = MaterialTheme.typography.headlineLarge,
                    color = DeepBlueDark
                )
                Text(
                    text = "Informações, direitos e transparência do projeto",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SlateBlue
                )
            }
        }

        item {
            MoreVersionCard(
                version = content.version,
                lastUpdated = content.lastUpdated
            )
        }

        items(
            items = content.sections,
            key = { it.id }
        ) { section ->
            MoreSectionCard(
                section = section,
                onClick = { onSectionClick(section.id) }
            )
        }
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
                .padding(14.dp),
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
            "mission" -> Icons.Filled.MenuBook
            else -> Icons.Filled.Info
        }

        MoreSectionTypeDto.RIGHTS_LIST -> Icons.Filled.GraphicEq
        MoreSectionTypeDto.ASSET_LIST -> Icons.Filled.Image
        MoreSectionTypeDto.PEOPLE_LIST -> Icons.Filled.MenuBook
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