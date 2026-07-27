package br.app.ide.ouvindoabiblia.ui.studies

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import br.app.ide.ouvindoabiblia.ui.theme.AppColors
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Lesson
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Study
import br.app.ide.ouvindoabiblia.ui.components.AppAsyncImage
import br.app.ide.ouvindoabiblia.ui.home.components.ErrorScreen
import br.app.ide.ouvindoabiblia.ui.home.components.LoadingScreen
import br.app.ide.ouvindoabiblia.ui.player.PlayerViewModel
import br.app.ide.ouvindoabiblia.ui.theme.Accent
import br.app.ide.ouvindoabiblia.ui.theme.OnAccent
import br.app.ide.ouvindoabiblia.ui.theme.OuvindoABibliaTheme

@Composable
fun StudyDetailsScreen(
    viewModel: StudyDetailsViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    bottomContentPadding: Dp = 0.dp,
    onBackClick: () -> Unit,
    onPlayStudy: (String, String, List<Lesson>, Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val studyTitle = viewModel.studyTitle

    // Verifica se o estudo atual é o que está tocando
    val playingIndex = if (playerState.title == studyTitle) {
        playerState.currentChapterIndex
    } else {
        -1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        when (val state = uiState) {
            is StudyDetailsUiState.Loading -> LoadingScreen()
            is StudyDetailsUiState.Error -> ErrorScreen(state.message) {
                viewModel.handle(
                    StudyDetailsIntent.Retry
                )
            }

            is StudyDetailsUiState.Success -> {
                StudyDetailsContent(
                    study = state.study,
                    playingIndex = playingIndex,
                    isAudioPlaying = playerState.isPlaying,
                    bottomContentPadding = bottomContentPadding,
                    onBackClick = onBackClick,
                    onPlayStudy = onPlayStudy
                )
            }
        }
    }
}

@Composable
private fun StudyDetailsContent(
    study: Study,
    playingIndex: Int,
    isAudioPlaying: Boolean,
    bottomContentPadding: Dp,
    onBackClick: () -> Unit,
    onPlayStudy: (String, String, List<Lesson>, Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        LessonsList(
            study = study,
            lessons = study.lessons,
            playingIndex = playingIndex,
            isAudioPlaying = isAudioPlaying,
            bottomContentPadding = bottomContentPadding,
            onBackClick = onBackClick,
            onPlayStudy = onPlayStudy
        )
    }
}

@Composable
private fun LessonsList(
    study: Study,
    lessons: List<Lesson>,
    playingIndex: Int,
    isAudioPlaying: Boolean,
    bottomContentPadding: Dp,
    onBackClick: () -> Unit,
    onPlayStudy: (String, String, List<Lesson>, Int) -> Unit
) {
    val horizontalSpacing = 0.dp

    val resolvedBottomPadding = if (bottomContentPadding == 0.dp) {
        horizontalSpacing + 16.dp
    } else {
        bottomContentPadding + horizontalSpacing
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = resolvedBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StudyDetailsHeader(
                study = study,
                lessons = lessons,
                onBackClick = onBackClick
            )
        }

        itemsIndexed(lessons, key = { _, item -> item.localId }) { index, lesson ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                LessonListItem(
                    index = index + 1,
                    lesson = lesson,
                    isCurrent = index == playingIndex,
                    isPlaying = isAudioPlaying,
                    onClick = { onPlayStudy(study.title, study.imageUrl, lessons, index) }
                )
            }
        }
    }
}

@Composable
fun LessonListItem(
    index: Int,
    lesson: Lesson,
    isCurrent: Boolean,   // é a aula carregada no player (borda/realce)
    isPlaying: Boolean,   // o áudio está tocando agora (pause vs play no ícone)
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = AppColors.card),
        border = if (isCurrent) BorderStroke(2.dp, Accent) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isCurrent) Accent else AppColors.badge),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCurrent) OnAccent else AppColors.onBadge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Exibe a duração formatada
                val minutes = lesson.duration / 60
                val seconds = lesson.duration % 60
                Text(
                    text = "${minutes}m ${seconds.toString().padStart(2, '0')}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textPrimary.copy(alpha = 0.8f)
                )

                // ISSUE 9.A: descrição da aula (texto longo do JSON) — recolhida em 2 linhas,
                // expande/recolhe no toque sem disparar o onClick do Card (o clickable interno
                // consome o gesto).
                visibleLessonDescription(lesson.description)?.let { description ->
                    var expanded by rememberSaveable(lesson.localId) { mutableStateOf(false) }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textPrimary.copy(alpha = 0.7f),
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            // O padding vem DEPOIS do clickable, então entra na área de
                            // toque: o texto (bodySmall, 2 linhas ≈ 34dp) sozinho ficava
                            // abaixo do alvo mínimo de 48dp.
                            .clickable { expanded = !expanded }
                            .padding(vertical = 8.dp)
                            .animateContentSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isCurrent) Accent else AppColors.textPrimary.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    // ISSUE 9.C: padrão de mercado — aula atual tocando mostra PAUSE
                    // (toque pausa); atual pausada ou outra aula mostram PLAY (toque
                    // retoma/troca). O antigo Replay prometia "reiniciar" e era no-op.
                    imageVector = if (isCurrent && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrent && isPlaying) "Pausar aula" else "Ouvir aula",
                    tint = if (isCurrent) OnAccent else AppColors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun StudyDetailsHeader(
    study: Study,
    lessons: List<Lesson>,
    onBackClick: () -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // O banco armazena em segundos, então somamos tudo
    val totalDurationSeconds = lessons.sumOf { it.duration }
    val totalMinutes = totalDurationSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    val durationLabel = when {
        hours > 0 && minutes > 0 -> "${hours}h${minutes.toString().padStart(2, '0')}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}min"
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            AppAsyncImage(
                imageUrl = study.imageUrl,
                contentDescription = study.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Surface(
                modifier = Modifier
                    .padding(start = 16.dp, top = statusBarPadding + 8.dp)
                    .size(40.dp)
                    .align(Alignment.TopStart),
                shape = CircleShape,
                color = AppColors.background.copy(alpha = 0.92f),
                tonalElevation = 0.dp,
                shadowElevation = 2.dp
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = AppColors.textPrimary
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = study.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary
            )

            Text(
                text = study.author,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = AppColors.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = study.description,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 22.sp,
                color = AppColors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudyMetaChip(text = "${lessons.size} áudios")
                StudyMetaChip(text = durationLabel)
            }
        }
    }
}

@Composable
private fun StudyMetaChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.heightIn(min = 28.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.card),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            width = 1.dp,
            color = AppColors.outline.copy(alpha = 0.72f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.textSecondary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

/**
 * ISSUE 9.A — descrição exibível de uma aula: trim + null se vazia/blank.
 * Função pura (testável) usada pelo [LessonListItem]; null = não renderiza o bloco.
 */
fun visibleLessonDescription(description: String?): String? =
    description?.trim()?.takeIf { it.isNotEmpty() }

// --- PREVIEWS ---

private fun previewStudyEntity(): Study {
    return Study(
        id = 1,
        title = "Estudos Expositivos em Apocalipse",
        author = "Rev. Leandro Lima",
        description = "Série de estudos bíblicos expositivos sobre o livro do Apocalipse, focando na esperança e soberania de Cristo.",
        imageUrl = "https://images.unsplash.com/photo-1504052434569-70ad5836ab65",
        lessons = previewLessonsList()
    )
}

private fun previewLessonsList(): List<Lesson> {
    return listOf(
        Lesson(
            localId = 1,
            remoteId = 1,
            studyId = 1,
            title = "A Revelação de Jesus Cristo",
            url = "https://example.com/audio1.mp3",
            duration = 3450, // 57m 30s
            isFavorite = false,
            description = "Aula introdutória que estabelece os fundamentos corretos para " +
                "interpretar o Apocalipse, enfatizando que se trata de uma revelação " +
                "simbólica e não especulativa."
        ),
        Lesson(
            localId = 2,
            remoteId = 2,
            studyId = 1,
            title = "A Igreja em Éfeso",
            url = "https://example.com/audio2.mp3",
            duration = 2805, // 46m 45s
            isFavorite = false,
            description = null
        ),
        Lesson(
            localId = 3,
            remoteId = 3,
            studyId = 1,
            title = "A Igreja em Esmirna",
            url = "https://example.com/audio3.mp3",
            duration = 3120, // 52m 00s
            isFavorite = false,
            description = null
        )
    )
}

private fun previewStudyWithLessons(): Study = previewStudyEntity()

@Preview(
    name = "Study details header",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412
)
@Composable
private fun PreviewStudyDetailsHeader() {
    OuvindoABibliaTheme {
        StudyDetailsHeader(
            study = previewStudyEntity(),
            lessons = previewLessonsList(),
            onBackClick = {}
        )
    }
}

@Preview(
    name = "Lesson item",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412
)
@Composable
private fun PreviewLessonListItem() {
    OuvindoABibliaTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.background)
                .padding(16.dp)
        ) {
            LessonListItem(
                index = 1,
                lesson = previewLessonsList().first(),
                isCurrent = false,
                isPlaying = false,
                onClick = {}
            )
        }
    }
}

@Preview(
    name = "Study details screen",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412,
    heightDp = 915
)
@Composable
private fun PreviewStudyDetailsContent() {
    OuvindoABibliaTheme {
        StudyDetailsContent(
            study = previewStudyWithLessons(),
            playingIndex = 1,
            isAudioPlaying = true,
            bottomContentPadding = 72.dp, // Simula o player aberto
            onBackClick = {},
            onPlayStudy = { _, _, _, _ -> }
        )
    }
}