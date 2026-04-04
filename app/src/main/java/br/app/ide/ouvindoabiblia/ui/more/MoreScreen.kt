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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.app.ide.ouvindoabiblia.ui.theme.Accent
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray
import br.app.ide.ouvindoabiblia.ui.theme.RosyBeige
import br.app.ide.ouvindoabiblia.ui.theme.SlateBlue
import com.google.android.datatransport.BuildConfig

private data class MoreItemUi(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: (() -> Unit)? = null
)

private data class MoreSectionUi(
    val title: String,
    val items: List<MoreItemUi>
)

@Composable
fun MoreScreen(
    bottomContentPadding: Dp = 0.dp,
    onAboutClick: () -> Unit = {},
    onMissionClick: () -> Unit = {},
    onAudioRightsClick: () -> Unit = {},
    onCoverRightsClick: () -> Unit = {},
    onCurationClick: () -> Unit = {},
    onContactClick: () -> Unit = {},
    onReportBugClick: () -> Unit = {},
    onSuggestImprovementClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onLicensesClick: () -> Unit = {}
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val resolvedBottomPadding = if (bottomContentPadding == 0.dp) {
        navBarPadding + 16.dp
    } else {
        bottomContentPadding
    }

    val sections = listOf(
        MoreSectionUi(
            title = "Sobre",
            items = listOf(
                MoreItemUi(
                    title = "Sobre o app",
                    subtitle = "Conheça o propósito e a proposta desta aplicação.",
                    icon = Icons.Filled.Info,
                    onClick = onAboutClick
                ),
                MoreItemUi(
                    title = "Missão",
                    subtitle = "O propósito cristão que orienta este projeto.",
                    icon = Icons.Filled.Favorite,
                    onClick = onMissionClick
                ),
                MoreItemUi(
                    title = "Versão do app",
                    subtitle = "Versão ${BuildConfig.VERSION_NAME}",
                    icon = Icons.Filled.Info,
                    onClick = null
                )
            )
        ),
        MoreSectionUi(
            title = "Conteúdo",
            items = listOf(
                MoreItemUi(
                    title = "Direitos dos áudios",
                    subtitle = "Informações sobre uso, fonte e responsabilidade dos áudios.",
                    icon = Icons.Filled.GraphicEq,
                    onClick = onAudioRightsClick
                ),
                MoreItemUi(
                    title = "Direitos das capas e imagens",
                    subtitle = "Créditos e observações sobre artes, capas e imagens.",
                    icon = Icons.Filled.Image,
                    onClick = onCoverRightsClick
                ),
                MoreItemUi(
                    title = "Curadoria de temas e estudos",
                    subtitle = "Como os conteúdos são organizados e apresentados no app.",
                    icon = Icons.Filled.MenuBook,
                    onClick = onCurationClick
                )
            )
        ),
        MoreSectionUi(
            title = "Suporte",
            items = listOf(
                MoreItemUi(
                    title = "Fale conosco",
                    subtitle = "Entre em contato para dúvidas gerais.",
                    icon = Icons.Filled.Email,
                    onClick = onContactClick
                ),
                MoreItemUi(
                    title = "Reportar problema",
                    subtitle = "Avise sobre erros, falhas ou comportamentos inesperados.",
                    icon = Icons.Filled.BugReport,
                    onClick = onReportBugClick
                ),
                MoreItemUi(
                    title = "Sugerir melhoria",
                    subtitle = "Envie ideias para evoluir a experiência do app.",
                    icon = Icons.Filled.Lightbulb,
                    onClick = onSuggestImprovementClick
                ),
                MoreItemUi(
                    title = "Compartilhar app",
                    subtitle = "Ajude outras pessoas a conhecerem este aplicativo.",
                    icon = Icons.Filled.Share,
                    onClick = onShareClick
                )
            )
        ),
        MoreSectionUi(
            title = "Legal",
            items = listOf(
                MoreItemUi(
                    title = "Política de privacidade",
                    subtitle = "Saiba como suas informações são tratadas.",
                    icon = Icons.Filled.Gavel,
                    onClick = onPrivacyClick
                ),
                MoreItemUi(
                    title = "Licenças open source",
                    subtitle = "Bibliotecas e componentes de terceiros utilizados no app.",
                    icon = Icons.Filled.Code,
                    onClick = onLicensesClick
                )
            )
        )
    )

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
                    fontWeight = FontWeight.Bold,
                    color = DeepBlueDark
                )
                Text(
                    text = "Informações, suporte e transparência do projeto",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SlateBlue
                )
            }
        }

        sections.forEach { section ->
            item {
                MoreSectionCard(section = section)
            }
        }

        item {
            Text(
                text = "Este aplicativo existe para incentivar a escuta da Palavra de Deus e o acesso a conteúdos cristãos edificantes.",
                style = MaterialTheme.typography.bodySmall,
                color = LavenderGray,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun MoreSectionCard(
    section: MoreSectionUi
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DeepBlueDark,
            modifier = Modifier.padding(start = 4.dp)
        )

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
            Column {
                section.items.forEachIndexed { index, item ->
                    MoreListItem(item = item)

                    if (index != section.items.lastIndex) {
                        HorizontalDivider(
                            color = RosyBeige.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreListItem(
    item: MoreItemUi
) {
    val clickableModifier = if (item.onClick != null) {
        Modifier.clickable { item.onClick.invoke() }
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            color = Accent.copy(alpha = 0.95f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = item.icon,
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
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = DeepBlueDark
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LavenderGray
            )
        }

        if (item.onClick != null) {
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