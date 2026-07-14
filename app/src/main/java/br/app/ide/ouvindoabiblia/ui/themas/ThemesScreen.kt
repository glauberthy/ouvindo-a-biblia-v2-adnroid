package br.app.ide.ouvindoabiblia.ui.themas

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Theme
import br.app.ide.ouvindoabiblia.ui.components.AppAsyncImage
import br.app.ide.ouvindoabiblia.ui.home.components.ErrorScreen
import br.app.ide.ouvindoabiblia.ui.home.components.LoadingScreen
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray
import br.app.ide.ouvindoabiblia.ui.theme.OuvindoABibliaTheme
import br.app.ide.ouvindoabiblia.ui.theme.RosyBeige
import br.app.ide.ouvindoabiblia.ui.theme.SlateBlue

@Composable
fun ThemesScreen(
    viewModel: ThemesViewModel = hiltViewModel(),
    bottomContentPadding: Dp = 0.dp,
    onThemeClick: (Int, String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val uiState = state) {
        is ThemesUiState.Loading -> LoadingScreen()
        is ThemesUiState.Error -> ErrorScreen(uiState.message) { viewModel.handle(ThemesIntent.Retry) }
        is ThemesUiState.Success -> {
            ThemesContent(
                themes = uiState.themes,
                bottomContentPadding = bottomContentPadding, // Pass the padding down
                onThemeClick = onThemeClick
            )
        }
    }
}

@Composable
private fun ThemesContent(
    themes: List<Theme>,
    bottomContentPadding: Dp, // Receive the padding
    onThemeClick: (Int, String) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val horizontalSpacing = 4.dp
    // Calculate resolved bottom padding
    val resolvedBottomPadding = if (bottomContentPadding == 0.dp) {
        horizontalSpacing + 16.dp
    } else {
        bottomContentPadding + horizontalSpacing
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(CreamBackground),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = statusBarPadding + 24.dp,
                bottom = resolvedBottomPadding // Use resolved padding
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)) {
                    Text(
                        text = "Temas Bíblicos",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlueDark
                    )
                    Text(
                        text = "Passagens selecionadas por assunto",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SlateBlue
                    )
                }
            }

            items(
                items = themes,
                key = { theme -> theme.id }
            ) { theme ->
                ThemeListItem(
                    theme = theme,
                    onClick = { onThemeClick(theme.id, theme.title) }
                )
            }
        }

    }
}

@Composable
fun ThemeListItem(
    theme: Theme,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ThemeCardPressAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple()
            ) { onClick() },
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeImage(theme)

            Spacer(modifier = Modifier.width(12.dp))

            ThemeTextContent(
                theme = theme,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Abrir tema",
                tint = RosyBeige.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


@Composable
private fun ThemeImage(theme: Theme) {
    Surface(
        modifier = Modifier.size(75.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        AppAsyncImage(
            imageUrl = theme.imageUrl,
            contentDescription = theme.title,
            modifier = Modifier
                .width(75.dp)
                .height(75.dp)
                .fillMaxSize()
                .padding(3.dp)
                .clip(shape = RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ThemeTextContent(
    theme: Theme,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = theme.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DeepBlueDark,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = theme.description,
            style = MaterialTheme.typography.bodyMedium,
            color = LavenderGray,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp
        )
    }
}

private fun previewTheme(
    id: Int,
    title: String,
    description: String,
    imageUrl: String
): Theme {
    return Theme(
        id = id,
        title = title,
        description = description,
        imageUrl = imageUrl
    )
}

private fun previewThemes(): List<Theme> = listOf(
    previewTheme(
        id = 1,
        title = "Ansiedade e confiança em Deus",
        description = "A ansiedade é combatida pela confiança na providência paternal de Deus, alimentada por Palavra e oração.",
        imageUrl = "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2"
    ),
    previewTheme(
        id = 2,
        title = "Arrependimento, Perdão e Justificação",
        description = "Culpa real é tratada pela graça real: Deus chama ao arrependimento e concede perdão com base na obra redentora.",
        imageUrl = "https://images.unsplash.com/photo-1504052434569-70ad5836ab65"
    ),
    previewTheme(
        id = 3,
        title = "Oração e Comunhão com Deus",
        description = "Oração é meio de graça: resposta filial ao Pai, em nome do Filho, pelo Espírito.",
        imageUrl = "https://images.unsplash.com/photo-1504052434569-70ad5836ab65"
    )
)

@Preview(
    name = "Theme item",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 360
)
@Composable
private fun PreviewThemeListItem() {
    OuvindoABibliaTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CreamBackground)
                .padding(20.dp)
        ) {
            ThemeListItem(
                theme = previewTheme(
                    id = 1,
                    title = "Ansiedade e confiança em Deus",
                    description = "A ansiedade é combatida pela confiança na providência paternal de Deus, alimentada por Palavra e oração.",
                    imageUrl = "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2"
                ),
                onClick = {}
            )
        }
    }
}

@Preview(
    name = "Themes screen",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412,
    heightDp = 915
)
@Composable
private fun PreviewThemesContent() {
    OuvindoABibliaTheme {
        ThemesContent(
            themes = previewThemes(),
            bottomContentPadding = 90.dp, // Test with padding
            onThemeClick = { _, _ -> }
        )
    }
}

@Preview(
    name = "Themes error",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412,
    heightDp = 915
)
@Composable
private fun PreviewThemesError() {
    OuvindoABibliaTheme {
        ErrorScreen(
            message = "Erro ao carregar temas."
        ) {}
    }
}