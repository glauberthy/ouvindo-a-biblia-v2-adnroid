package br.app.ide.ouvindoabiblia.ui.studies


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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.ide.ouvindoabiblia.data.local.entity.StudyEntity
import br.app.ide.ouvindoabiblia.data.local.entity.StudyLessonEntity
import br.app.ide.ouvindoabiblia.data.local.model.StudyWithLessons
import br.app.ide.ouvindoabiblia.ui.components.AppAsyncImage
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
fun StudiesScreen(
    onStudyClick: (Int, String) -> Unit,
    bottomContentPadding: Dp = 0.dp,
    viewModel: StudiesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val uiState = state) {
        is StudiesUiState.Loading -> LoadingScreen()
        is StudiesUiState.Empty -> EmptyStudiesScreen()
        is StudiesUiState.Error -> ErrorScreen(uiState.message) {
            viewModel.handle(StudiesIntent.Retry)
        }

        is StudiesUiState.Success -> {
            StudiesContent(
                studies = uiState.studies,
                onStudyClick = onStudyClick,
                bottomContentPadding = bottomContentPadding
            )
        }
    }
}

@Composable
private fun StudiesContent(
    studies: List<StudyWithLessons>,
    onStudyClick: (Int, String) -> Unit,
    bottomContentPadding: Dp
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {

        val horizontalScreenPadding = 20.dp
        val gridSpacing = 16.dp
        val bottomBreathingRoom = 4.dp

        val resolvedBottomPadding =
            if (bottomContentPadding == 0.dp) {
                horizontalScreenPadding
            } else {
                bottomContentPadding + bottomBreathingRoom
            }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalScreenPadding,
                end = horizontalScreenPadding,
                top = 24.dp,
                bottom = resolvedBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(gridSpacing)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(
                        top = statusBarPadding + 8.dp,
                        bottom = 8.dp
                    )
                ) {
                    Text(
                        text = "Estudos Bíblicos",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlueDark
                    )
                    Text(
                        text = "Séries de exposições em áudio",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SlateBlue
                    )
                }
            }

            items(
                items = studies,
                key = { it.study.id }
            ) { study ->
                StudyListItem(
                    study = study,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onStudyClick(study.study.id, study.study.title) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarPadding + 56.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CreamBackground.copy(alpha = 0.98f),
                            CreamBackground.copy(alpha = 0.88f),
                            CreamBackground.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}


@Composable
private fun StudyListItem(
    study: StudyWithLessons,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.99f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "StudyListItemScale"
    )

    val audioCount = study.lessons.size
    val totalDurationSeconds = study.lessons.sumOf { it.duration }
    val audioCountLabel = formatAudioCount(audioCount)
    val totalDurationLabel = formatTotalDuration(totalDurationSeconds)

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple()
            ) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFCFA)
        ),
        shape = RoundedCornerShape(22.dp),
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(75.dp),
                        shape = CircleShape,
                        color = Accent.copy(alpha = 0.14f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Accent.copy(alpha = 0.90f)
                        ),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        AppAsyncImage(
                            imageUrl = study.study.imageUrl,
                            contentDescription = "Autor ${study.study.author}",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = study.study.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepBlueDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = study.study.author,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.W400,
                            color = SlateBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudyMetaChip(text = audioCountLabel)
                    StudyMetaChip(text = totalDurationLabel)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = study.study.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LavenderGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Abrir estudo",
                tint = Accent.copy(alpha = 0.90f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun StudyMetaChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 24.dp),
        shape = RoundedCornerShape(999.dp),
        color = Accent.copy(alpha = 0.90f),
        border = BorderStroke(
            width = 1.dp,
            color = RosyBeige.copy(alpha = 0.72f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = SlateBlue,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

private fun formatAudioCount(count: Int): String {
    return if (count == 1) "1 áudio" else "$count áudios"
}

private fun formatTotalDuration(totalSeconds: Long): String {
    val totalMinutes = totalSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h${minutes.toString().padStart(2, '0')}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}min"
    }
}


@Composable
private fun EmptyStudiesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.LibraryBooks,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = RosyBeige
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Nenhum estudo disponível",
            style = MaterialTheme.typography.titleMedium,
            color = SlateBlue,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Quando novos estudos estiverem disponíveis,\neles aparecerão aqui para você ouvir.",
            color = LavenderGray,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


//preview

private fun previewStudy(
    id: Int,
    title: String,
    author: String,
    description: String
): StudyWithLessons {
    return StudyWithLessons(
        study = StudyEntity(
            id = id,
            title = title,
            author = author,
            description = description,
            imageUrl = "https://randomuser.me/api/portraits/men/11.jpg"
        ),
        lessons = listOf(
            StudyLessonEntity(
                localId = 1,
                remoteId = 1,
                studyId = id,
                title = "Capítulo 1",
                url = "https://example.com/audio1.mp3",
                duration = 5886
            ),
            StudyLessonEntity(
                localId = 2,
                remoteId = 2,
                studyId = id,
                title = "Capítulo 2",
                url = "https://example.com/audio2.mp3",
                duration = 2400
            )
        )
    )
}

private fun previewStudies(): List<StudyWithLessons> = listOf(
    previewStudy(
        id = 1,
        title = "Estudos Expositivos em Apocalipse",
        author = "Rev. Leandro Lima",
        description = "Série de estudos bíblicos expositivos sobre o Apocalipse."
    ),
    previewStudy(
        id = 2,
        title = "Caminhando por Provérbios",
        author = "Pr. Marcos Silva",
        description = "Sabedoria milenar aplicada aos dilemas dos dias de hoje."
    ),
    previewStudy(
        id = 3,
        title = "A Graça que Transforma",
        author = "Dra. Helena Souza",
        description = "Um mergulho profundo nos primeiros capítulos da carta aos Romanos."
    )
)


@Preview(
    name = "Study item",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 360
)
@Composable
private fun PreviewStudyListItem() {
    OuvindoABibliaTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CreamBackground)
                .padding(20.dp)
        ) {
            StudyListItem(
                study = previewStudy(
                    id = 1,
                    title = "Estudos Expositivos em Apocalipse",
                    author = "Rev. Leandro Lima",
                    description = "Série de estudos bíblicos expositivos sobre o Apocalipse."
                ),
                modifier = Modifier.fillMaxWidth(),
                onClick = {}
            )
        }
    }
}


@Preview(
    name = "Studies screen",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412,
    heightDp = 915
)
@Composable
private fun PreviewStudiesContent() {
    OuvindoABibliaTheme {
        StudiesContent(
            studies = previewStudies(),
            onStudyClick = { _, _ -> },
            bottomContentPadding = 72.dp
        )
    }
}

@Preview(
    name = "Studies empty",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412,
    heightDp = 915
)
@Composable
private fun PreviewEmptyStudiesScreen() {
    OuvindoABibliaTheme {
        EmptyStudiesScreen()
    }
}