package br.app.ide.ouvindoabiblia.ui.themas

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.ide.ouvindoabiblia.data.local.entity.ThemeEntity
import br.app.ide.ouvindoabiblia.ui.components.AppAsyncImage
import br.app.ide.ouvindoabiblia.ui.home.components.ErrorScreen
import br.app.ide.ouvindoabiblia.ui.home.components.LoadingScreen
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray

@Composable
fun ThemesScreen(
    viewModel: ThemesViewModel = hiltViewModel(),
    onThemeClick: (Int, String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val uiState = state) {
        is ThemesUiState.Loading -> LoadingScreen()
        is ThemesUiState.Error -> ErrorScreen(uiState.message) { viewModel.handle(ThemesIntent.Retry) }
        is ThemesUiState.Success -> {
            ThemesContent(
                themes = uiState.themes,
                onThemeClick = onThemeClick
            )
        }
    }
}

@Composable
private fun ThemesContent(
    themes: List<ThemeEntity>,
    onThemeClick: (Int, String) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = statusBarPadding + 16.dp,
            bottom = navBarPadding + 56.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Cura Bíblica",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DeepBlueDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // ALTERADO: items para itemsIndexed para calcular par ou ímpar
        itemsIndexed(themes, key = { _, theme -> theme.id }) { index, theme ->
            val isImageOnLeft = index % 2 == 0

            ThemeListItem(
                theme = theme,
                isImageOnLeft = isImageOnLeft, // Passando o comando de layout
                onClick = { onThemeClick(theme.id, theme.title) }
            )
        }
    }
}

@Composable
fun ThemeListItem(
    theme: ThemeEntity,
    isImageOnLeft: Boolean, // ADICIONADO
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "CardPressAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple()
            ) { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 1.dp else 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ALTERADO: Lógica do Zig-Zag
            if (isImageOnLeft) {
                ThemeImage(theme)
                Spacer(modifier = Modifier.width(16.dp))
                ThemeTextContent(theme, Modifier.weight(1f))
            } else {
                ThemeTextContent(theme, Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                ThemeImage(theme)
            }
        }
    }
}


@Composable
private fun ThemeImage(theme: ThemeEntity) {
    AppAsyncImage(
        imageUrl = theme.imageUrl,
        contentDescription = theme.title,
        modifier = Modifier
            .width(90.dp)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun ThemeTextContent(theme: ThemeEntity, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = theme.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DeepBlueDark,
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