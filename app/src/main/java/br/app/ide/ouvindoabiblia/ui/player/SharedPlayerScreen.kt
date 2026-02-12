package br.app.ide.ouvindoabiblia.ui.player

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import br.app.ide.ouvindoabiblia.R
import br.app.ide.ouvindoabiblia.ui.player.components.ChaptersSheet
import br.app.ide.ouvindoabiblia.ui.player.components.SleepTimerBottomSheet
import br.app.ide.ouvindoabiblia.ui.player.components.SpeedBottomSheet
import br.app.ide.ouvindoabiblia.ui.theme.Accent
import br.app.ide.ouvindoabiblia.ui.theme.isDark
import coil.compose.AsyncImage
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
fun SharedPlayerScreen(
    expandProgress: Float,
    uiState: PlayerUiState,
    backgroundColor: Color,
    onPlayPause: () -> Unit,
    onSkipToNextChapter: () -> Unit,
    onSkipToPreviousChapter: () -> Unit,
    onRewind: () -> Unit,
    onFastForward: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onCollapse: () -> Unit,
    onSeek: (Long) -> Unit,
    onShare: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onChapterSelect: (Int) -> Unit,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    // ESTADO
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showChapters by remember { mutableStateOf(false) }

    val currentChapter = uiState.chapters.getOrNull(uiState.currentChapterIndex)
    val isFavorite = currentChapter?.chapter?.isFavorite == true
    val hasMedia = uiState.title.isNotEmpty()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidth = this.maxWidth

        // --- GEOMETRIA ---
        val miniHeight = 56.dp
        val miniWidth = miniHeight * 0.7f
        val fullWidth = screenWidth * 0.65f
        val fullHeight = fullWidth / 0.7f

        val currentWidth = lerp(miniWidth, fullWidth, expandProgress)
        val currentHeight = lerp(miniHeight, fullHeight, expandProgress)

        val imageStartX = 16.dp
        val imageEndX = (screenWidth - fullWidth) / 2
        val currentX = lerp(imageStartX, imageEndX, expandProgress)

        val imageStartY = 4.dp
        val imageEndY = 100.dp
        val currentY = lerp(imageStartY, imageEndY, expandProgress)

        val imageCorner = lerp(4.dp, 12.dp, expandProgress)
        val imageShadow = lerp(2.dp, 16.dp, expandProgress)

        // Cores
        val isBackgroundDark = backgroundColor.isDark()
        val headerContentColor = if (isBackgroundDark) Color.White else Color.Black
        val miniContentColor = if (isBackgroundDark) Color.White else Color.Black
        val playerControlsColor = Color.White
        val playerSecondaryColor = Color.LightGray


        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(expandProgress)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(backgroundColor, Color(0xFF22223B))
                    )
                )
        )

        // --- FULL PLAYER ---
        if (expandProgress > 0.1f) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = imageEndY + fullHeight + 24.dp)
                    .padding(horizontal = 24.dp)
                    .alpha(expandProgress),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ROW 1: TÍTULO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {

                        // 2. O TÍTULO (O Livro e o Capítulo)
                        // Ex: "Gênesis 1"
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.headlineMedium, // Aumentei para Headline
                            fontWeight = FontWeight.Bold,
                            color = playerControlsColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // 3. O SUBTÍTULO (O detalhe)
                        // Ex: "Capítulo 1"
                        Text(
                            text = uiState.subtitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = playerSecondaryColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favoritar",
                            tint = if (isFavorite) playerControlsColor else playerControlsColor.copy(
                                alpha = 0.7f
                            ),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                PlayerProgressBar(
                    currentPosition = uiState.currentPosition,
                    duration = uiState.duration,
                    onSeek = onSeek
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showSpeedSheet = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text(
                            text = "${"%.1f".format(uiState.playbackSpeed)}x",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = playerControlsColor
                        )
                    }

                    IconButton(onClick = onRewind, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Replay,
                                contentDescription = "-10s",
                                tint = playerControlsColor,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "10",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = playerControlsColor,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                    }


                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(72.dp),
                        onClick = onPlayPause
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (uiState.isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = Color.Black,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onFastForward, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Replay,
                                contentDescription = "-10s",
                                tint = playerControlsColor,
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(scaleX = -1f, scaleY = 1f)
                            )
                            Text(
                                text = "30",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = playerControlsColor,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showSleepTimerSheet = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NightlightRound,
                            contentDescription = "Sleep",
                            tint = playerControlsColor,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        onClick = { showChapters = true },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 32.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "CAPÍTULOS",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                }
            }
        }

        // --- HEADER (Minimizar) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                .alpha(expandProgress)
        ) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    "Minimizar",
                    tint = headerContentColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "OUVINDO A BÍBLIA",
                color = headerContentColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.Center)
            )

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
//                CastButton(
//                    color = headerContentColor,
//                    modifier = Modifier.padding(end = 8.dp)
//                )

                IconButton(onClick = onShare) {
                    Icon(
                        painter = painterResource(id = R.drawable.share_24px),
                        contentDescription = "Share",
                        tint = headerContentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }


        }

        // --- MINI PLAYER ---
        if (expandProgress < 0.9f && hasMedia) {
            val miniAlpha = 1f - (expandProgress * 3).coerceIn(0f, 1f)
            if (miniAlpha > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .alpha(miniAlpha)
                        .padding(start = 16.dp + miniWidth + 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpen() },
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (uiState.isBuffering && uiState.title.isEmpty()) "Carregando..." else uiState.title.ifEmpty { "Aguarde..." },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = miniContentColor,
                            maxLines = 1
                        )
                        Text(
                            text = uiState.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = miniContentColor.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Fav",
                            tint = miniContentColor
                        )
                    }

                    IconButton(onClick = onSkipToPreviousChapter) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            "Prev",
                            tint = miniContentColor
                        )
                    }

                    IconButton(onClick = onPlayPause) {
                        if (uiState.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = miniContentColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = miniContentColor
                            )
                        }
                    }
                    IconButton(onClick = onSkipToNextChapter) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            "Prox",
                            tint = miniContentColor
                        )
                    }
                }
            }
        }

        // --- CAPA ANIMADA ---
        Surface(
            modifier = Modifier
                .offset {
                    IntOffset(
                        with(density) { currentX.toPx().roundToInt() },
                        with(density) { currentY.toPx().roundToInt() })
                }
                .size(width = currentWidth, height = currentHeight)
                .clickable(enabled = expandProgress < 0.5f) { onOpen() }
                .shadow(imageShadow, RoundedCornerShape(imageCorner)),
            shape = RoundedCornerShape(imageCorner),
            color = Color.DarkGray
        ) {
            if (uiState.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = uiState.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.background(Color.Gray)
                ) {
                    Icon(Icons.Rounded.MusicNote, null, tint = Color.White)
                }
            }
        }
    }

    // SHEETS (Mantidos iguais)
    if (showChapters) {
        ChaptersSheet(
            chapters = uiState.chapters,
            currentIndex = uiState.currentChapterIndex,
            accentColor = Accent,
            onChapterClick = { index -> onChapterSelect(index) },
            onDismiss = { showChapters = false }
        )
    }

    if (showSpeedSheet) {
        SpeedBottomSheet(
            currentSpeed = uiState.playbackSpeed,
            onSpeedSelected = { newSpeed -> onSetSpeed(newSpeed) },
            accentColor = Accent,
            onDismiss = { showSpeedSheet = false }
        )
    }

    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            accentColor = Accent,
            currentMinutes = uiState.activeSleepTimerMinutes,
            onTimeSelected = { minutes -> onSetSleepTimer(minutes) },
            onDismiss = { showSleepTimerSheet = false }
        )
    }
}


@Composable
fun PlayerProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Evita divisão por zero e garante range válido
    val safeDuration = if (duration > 0) duration else 1L

    // Estado local para controlar o arraste (Drag) do slider
    // Sem isso, o slider fica "pulando" enquanto você tenta arrastar
    var isDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    // Se estiver arrastando, mostra a posição do dedo. Se não, mostra a posição real do áudio.
    val contentPosition = if (isDragging) sliderPosition else currentPosition.toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = contentPosition,
            onValueChange = { newPos ->
                isDragging = true
                sliderPosition = newPos
            },
            onValueChangeFinished = {
                onSeek(sliderPosition.toLong())
                isDragging = false
            },
            valueRange = 0f..safeDuration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(contentPosition.toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )

            Text(
                text = formatTime(safeDuration),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Função utilitária para formatar milissegundos em MM:SS ou HH:MM:SS
@SuppressLint("DefaultLocale")
private fun formatTime(ms: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}


@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Mini Player (Collapsed)"
)
@Composable
fun SharedPlayerScreenMiniPreview() {
    val mockUiState = PlayerUiState(
        title = "Gênesis 1",
        subtitle = "Capítulo 1",
        imageUrl = "", // Pode deixar vazio ou colocar uma URL fake se tiver um placeholder
        isPlaying = false,
        currentPosition = 15000L,
        duration = 60000L
    )

    SharedPlayerScreen(
        expandProgress = 0f, // 0f = Minimizado (Mini Player)
        uiState = mockUiState,
        backgroundColor = Color(0xFFFFCC00), // Uma cor de exemplo (Amarelo)
        onPlayPause = {},
        onSkipToNextChapter = {},
        onSkipToPreviousChapter = {},
        onRewind = {},
        onFastForward = {},
        onSetSleepTimer = {},
        onCollapse = {},
        onSeek = {},
        onShare = {},
        onSetSpeed = {},
        onChapterSelect = {},
        onOpen = {},
        onToggleFavorite = {}
    )
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Full Player (Expanded)",
    heightDp = 800
)
@Composable
fun SharedPlayerScreenFullPreview() {
    val mockUiState = PlayerUiState(
        title = "Gênesis 1",
        subtitle = "Capítulo 1",
        imageUrl = "",
        isPlaying = true,
        currentPosition = 30000L,
        duration = 60000L,
        playbackSpeed = 1.5f
    )

    SharedPlayerScreen(
        expandProgress = 1f, // 1f = Maximizado (Tela Cheia)
        uiState = mockUiState,
        backgroundColor = Accent,
        onPlayPause = {},
        onSkipToNextChapter = {},
        onSkipToPreviousChapter = {},
        onRewind = {},
        onFastForward = {},
        onSetSleepTimer = {},
        onCollapse = {},
        onSeek = {},
        onShare = {},
        onSetSpeed = {},
        onChapterSelect = {},
        onOpen = {},
        onToggleFavorite = {}
    )
}