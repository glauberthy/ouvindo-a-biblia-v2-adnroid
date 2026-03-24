package br.app.ide.ouvindoabiblia.ui.studies

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.ide.ouvindoabiblia.data.local.entity.StudyEntity
import br.app.ide.ouvindoabiblia.ui.components.AppAsyncImage
import br.app.ide.ouvindoabiblia.ui.home.components.ErrorScreen
import br.app.ide.ouvindoabiblia.ui.home.components.LoadingScreen
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray
import br.app.ide.ouvindoabiblia.ui.theme.SlateBlue

@Composable
fun StudiesScreen(
    viewModel: StudiesViewModel = hiltViewModel(),
    onStudyClick: (Int, String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val uiState = state) {
        is StudiesUiState.Loading -> LoadingScreen()
        is StudiesUiState.Error -> ErrorScreen(uiState.message) { viewModel.handle(StudiesIntent.Retry) }
        is StudiesUiState.Success -> {
            StudiesContent(
                studies = uiState.studies,
                onStudyClick = onStudyClick
            )
        }
    }
}

@Composable
private fun StudiesContent(
    studies: List<StudyEntity>,
    onStudyClick: (Int, String) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = statusBarPadding + 24.dp,
            bottom = navBarPadding + 56.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Estudos Bíblicos",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlueDark
                )
                Text(
                    text = "Séries de exposições em áudio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateBlue
                )
            }
        }

        items(studies, key = { it.id }) { study ->
            StudyListItem(
                study = study,
                onClick = { onStudyClick(study.id, study.title) }
            )
        }
    }
}

@Composable
fun StudyListItem(
    study: StudyEntity,
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
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Imagem cobrindo o topo do card (aspectRatio 16:9 como um banner de vídeo)
            AppAsyncImage(
                imageUrl = study.imageUrl,
                contentDescription = study.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop // Garante que a imagem preencha o espaço
            )

            // Conteúdo textual abaixo da imagem
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = study.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlueDark,
                    lineHeight = 24.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Linha com o autor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Autor",
                        tint = SlateBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = study.author,
                        style = MaterialTheme.typography.labelLarge,
                        color = SlateBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = study.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LavenderGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
        }
    }
}