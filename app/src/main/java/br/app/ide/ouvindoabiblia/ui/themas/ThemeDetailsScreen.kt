package br.app.ide.ouvindoabiblia.ui.themas

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Moment
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Theme
import br.app.ide.ouvindoabiblia.ui.components.AppAsyncImage
import br.app.ide.ouvindoabiblia.ui.home.components.ErrorScreen
import br.app.ide.ouvindoabiblia.ui.home.components.LoadingScreen
import br.app.ide.ouvindoabiblia.ui.player.PlayerViewModel
import br.app.ide.ouvindoabiblia.ui.theme.Accent
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray
import br.app.ide.ouvindoabiblia.ui.theme.OuvindoABibliaTheme
import br.app.ide.ouvindoabiblia.ui.theme.RosyBeige
import br.app.ide.ouvindoabiblia.ui.theme.SlateBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDetailsScreen(
    viewModel: ThemeDetailsViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    bottomContentPadding: Dp = 0.dp,
    onBackClick: () -> Unit,
    onPlayTheme: (List<Moment>, Int, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val themeTitle = viewModel.themeTitle

    val playingIndex = if (playerState.title == themeTitle) {
        playerState.currentChapterIndex
    } else {
        -1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        when (val state = uiState) {
            is ThemeDetailsUiState.Loading -> LoadingScreen()
            is ThemeDetailsUiState.Error -> ErrorScreen(state.message) {
                viewModel.handle(ThemeDetailsIntent.Retry)
            }
            is ThemeDetailsUiState.Success -> {
                ThemeDetailsContent(
                    theme = state.theme,
                    moments = state.moments,
                    playingIndex = playingIndex,
                    isAudioPlaying = playerState.isPlaying,
                    bottomContentPadding = bottomContentPadding,
                    onBackClick = onBackClick,
                    onPlayTheme = onPlayTheme
                )
            }
        }
    }
}

@Composable
private fun MomentsList(
    theme: Theme,
    moments: List<Moment>,
    playingIndex: Int,
    isAudioPlaying: Boolean,
    bottomContentPadding: Dp,
    onBackClick: () -> Unit,
    onPlayTheme: (List<Moment>, Int, String) -> Unit
) {
    // Igual ao padding horizontal do seu MomentListItem
    val horizontalSpacing = 0.dp

    // Se não tiver player (0.dp), usa só o respiro. Se tiver, soma o player + respiro.
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
            ThemeDetailsHeader(
                theme = theme,
                moments = moments,
                onBackClick = onBackClick
            )
        }

        itemsIndexed(moments, key = { _, item -> item.id }) { index, momentAudio ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                MomentListItem(
                    index = index + 1,
                    item = momentAudio,
                    isCurrent = index == playingIndex,
                    isPlaying = isAudioPlaying,
                    onClick = { onPlayTheme(moments, index, theme.title) }
                )
            }
        }
    }
}

@Composable
fun MomentListItem(
    index: Int,
    item: Moment,
    isCurrent: Boolean,   // é o momento carregado no player (borda/realce)
    isPlaying: Boolean,   // o áudio está tocando agora (pause vs play no ícone)
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        // Fundo sempre branco
        colors = CardDefaults.cardColors(containerColor = Color.White),
        // Adiciona borda colorida apenas se estiver tocando
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, Accent) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Círculo do Número (Ex: "4")
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    // Cor de fundo do número muda se estiver tocando
                    .background(if (isCurrent) Accent else DeepBlueDark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    // Cor do texto do número
                    color = if (isCurrent) DeepBlueDark else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos (Mantidos sem alteração de cor de fundo)
            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = item.reference,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlueDark
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepBlueDark.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Círculo do Ícone de Play (Ex: ">")
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    // Cor de fundo do Play muda se estiver tocando
                    .background(if (isCurrent) Accent else DeepBlueDark.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                // ISSUE 9.C (contexto de Tema): padrão de mercado — momento atual tocando
                // mostra PAUSE (toque pausa); pausado/outro momento mostram PLAY.
                Icon(
                    imageVector = if (isCurrent && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrent && isPlaying) "Pausar Versículo" else "Ouvir Versículo",
                    tint = DeepBlueDark
                )
            }
        }
    }
}


@Composable
private fun ThemeDetailsHeader(
    theme: Theme,
    moments: List<Moment>,
    onBackClick: () -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val totalDurationMs = moments.sumOf {
        (it.endMs - it.startMs).coerceAtLeast(0L)
    }

    val totalMinutes = (totalDurationMs / 1000L / 60L).toInt()
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
                imageUrl = theme.imageUrl,
                contentDescription = theme.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Surface(
                modifier = Modifier
                    .padding(start = 16.dp, top = statusBarPadding + 8.dp)
                    .size(40.dp)
                    .align(Alignment.TopStart),
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
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = theme.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DeepBlueDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = theme.description,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 22.sp,
                color = LavenderGray
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeMetaChip(text = "${moments.size} áudios")
                ThemeMetaChip(text = durationLabel)
            }
        }
    }
}

@Composable
private fun ThemeMetaChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.heightIn(min = 28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            width = 1.dp,
            color = RosyBeige.copy(alpha = 0.72f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = SlateBlue,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun ThemeDetailsContent(
    theme: Theme,
    moments: List<Moment>,
    playingIndex: Int,
    isAudioPlaying: Boolean,
    bottomContentPadding: Dp,
    onBackClick: () -> Unit,
    onPlayTheme: (List<Moment>, Int, String) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {

        MomentsList(
            theme = theme,
            moments = moments,
            playingIndex = playingIndex,
            isAudioPlaying = isAudioPlaying,
            bottomContentPadding = bottomContentPadding,
            onBackClick = onBackClick,
            onPlayTheme = onPlayTheme
        )


    }
}

private fun previewTheme(): Theme {
    return Theme(
        id = 1,
        title = "Ansiedade e confiança em Deus",
        description = "A ansiedade é combatida pela confiança na providência paternal de Deus, alimentada por Palavra e oração.",
        imageUrl = "https://images.unsplash.com/photo-1504052434569-70ad5836ab65"
    )
}

@Preview(
    name = "Theme details header",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412
)
@Composable
private fun PreviewThemeDetailsHeader() {
    OuvindoABibliaTheme {
        ThemeDetailsHeader(
            theme = previewThemeEntity(),
            moments = previewMomentsList(),
            onBackClick = {}
        )
    }
}

@Preview(
    name = "Moment item",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412
)
@Composable
private fun PreviewMomentListItem() {
    OuvindoABibliaTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CreamBackground)
                .padding(16.dp)
        ) {
            MomentListItem(
                index = 1,
                item = previewMomentsList().first(),
                isCurrent = false,
                isPlaying = false,
                onClick = {}
            )
        }
    }
}

@Preview(
    name = "Theme details screen",
    showBackground = true,
    backgroundColor = 0xFFF2E9E4,
    widthDp = 412,
    heightDp = 915
)
@Composable
private fun PreviewThemeDetailsContent() {
    OuvindoABibliaTheme {
        ThemeDetailsContent(
            theme = previewThemeEntity(),
            moments = previewMomentsList(),
            playingIndex = 1,
            isAudioPlaying = true,
            bottomContentPadding = 72.dp, // <-- SIMULA O PLAYER ABERTO
            onBackClick = {},
            onPlayTheme = { _, _, _ -> }
        )
    }
}


private fun previewThemeEntity(): Theme {
    return Theme(
        id = 1,
        title = "Ansiedade e confiança em Deus",
        description = "A ansiedade é combatida pela confiança na providência paternal de Deus, alimentada por Palavra e oração.",
        imageUrl = "https://images.unsplash.com/photo-1504052434569-70ad5836ab65"
    )
}

private fun previewMomentWithAudio(
    id: Long,
    reference: String,
    title: String,
    startMs: Long,
    endMs: Long,
    bookId: Int = 1,
    chapterNumber: Int = 1,
    themeId: Int = 1,
    audioUrl: String = "https://example.com/audio.mp3",
    bookName: String = "Mateus",
    coverUrl: String? = null
): Moment {
    return Moment(
        id = id,
        themeId = themeId,
        bookId = bookId,
        chapterNumber = chapterNumber,
        title = title,
        startMs = startMs,
        endMs = endMs,
        reference = reference,
        audioUrl = audioUrl,
        bookName = bookName,
        coverUrl = coverUrl
    )
}

private fun previewMomentsList(): List<Moment> {
    return listOf(
        previewMomentWithAudio(
            id = 1,
            reference = "Mateus 6:25-34",
            title = "Não andeis ansiosos pela vossa vida",
            startMs = 0L,
            endMs = 180000L,
            bookName = "Mateus"
        ),
        previewMomentWithAudio(
            id = 2,
            reference = "Filipenses 4:6-7",
            title = "Não andeis ansiosos por coisa alguma",
            startMs = 0L,
            endMs = 150000L,
            bookName = "Filipenses"
        ),
        previewMomentWithAudio(
            id = 3,
            reference = "Salmos 56:3",
            title = "Em me vindo o temor, hei de confiar em ti",
            startMs = 0L,
            endMs = 90000L,
            bookName = "Salmos"
        )
    )
}