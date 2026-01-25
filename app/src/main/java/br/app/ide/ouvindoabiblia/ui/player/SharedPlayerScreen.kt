package br.app.ide.ouvindoabiblia.ui.player

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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import br.app.ide.ouvindoabiblia.R
import br.app.ide.ouvindoabiblia.ui.player.components.CastButton
import br.app.ide.ouvindoabiblia.ui.player.components.SleepTimerBottomSheet
import br.app.ide.ouvindoabiblia.ui.player.components.SpeedBottomSheet
import br.app.ide.ouvindoabiblia.ui.theme.isDark
import coil.compose.AsyncImage
import kotlin.math.roundToInt

@Composable
fun SharedPlayerScreen(
    expandProgress: Float,
    uiState: PlayerUiState,
    backgroundColor: Color,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,       // Era onSkipPrev
    onFastForward: () -> Unit,  // Era onSkipNext
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
                        colors = listOf(backgroundColor, Color(0xFF22223B)) // Fundo bem dark
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
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = playerControlsColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = uiState.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = playerSecondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Botão Favorito (Estilo Fino/Vazado)
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(48.dp) // Área de toque
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

                // ROW 2: PROGRESSO
                PlayerProgressBar(
                    currentPosition = uiState.currentPosition,
                    duration = uiState.duration,
                    onSeek = onSeek
                )

                Spacer(modifier = Modifier.height(24.dp)) // Aumentei um pouco o espaçamento

                // === ROW 3: CONTROLES DE PLAYBACK (Aumentados) ===
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. VELOCIDADE
                    IconButton(
                        onClick = { showSpeedSheet = true },
                        modifier = Modifier.size(48.dp) // Área de toque maior
                    ) {
                        Text(
                            text = "${"%.1f".format(uiState.playbackSpeed)}x",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = playerControlsColor
                        )
                    }


                    // 2. VOLTAR 10s (Bem Grande para ler o número)
                    IconButton(onClick = onRewind, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Replay, // Rounded é mais clean que Default
                                contentDescription = "-10s",
                                tint = playerControlsColor,
                                modifier = Modifier.size(36.dp)
                            )
                            // Texto "10" bem pequeno dentro
                            Text(
                                text = "10",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = playerControlsColor,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                    }

                    // 3. PLAY/PAUSE (Rei da Tela)
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(72.dp),
                        onClick = onPlayPause
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    IconButton(onClick = onFastForward, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Replay, // Rounded é mais clean que Default
                                contentDescription = "-10s",
                                tint = playerControlsColor,
                                modifier = Modifier
                                    .size(36.dp)
                                    .scale(scaleX = -1f, scaleY = 1f)
                            )
                            // Texto "10" bem pequeno dentro
                            Text(
                                text = "30",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = playerControlsColor,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                    }

                    // 5. SLEEP (Lua)
                    IconButton(
                        onClick = { showSleepTimerSheet = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            // Outlined.Nightlight é apenas o contorno da lua (o que você queria)
                            imageVector = Icons.Rounded.NightlightRound,
                            contentDescription = "Sleep",
                            tint = playerControlsColor,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Empurra tudo para baixo
                Spacer(modifier = Modifier.weight(1f))

                // === ROW 4: BOTÃO CAPÍTULOS ===
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        onClick = { showChapters = true },
                        shape = CircleShape,
                        // Fundo levemente mais claro
                        color = Color.White.copy(alpha = 0.2f),
                        // Borda mais forte para definir o limite
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
                                tint = Color.White, // Branco Puro para destaque
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "CAPÍTULOS",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White, // Branco Puro
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
                .height(80.dp) // Altura fixa e segura
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                .alpha(expandProgress)
        ) {
            // 1. ESQUERDA: Minimizar
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.align(Alignment.CenterStart) // Fixado na esquerda
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    "Minimizar",
                    tint = headerContentColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            // 2. CENTRO: Título (Absoluto)
            // O Box permite que este texto fique EXATAMENTE no meio,
            // ignorando se tem 1 ou 3 botões nas laterais.
            Text(
                text = "OUVINDO A BÍBLIA",
                color = headerContentColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.Center)
            )

            // 3. DIREITA: Ações de Sistema
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cast (Prioridade 1 de Hardware)
//                IconButton(onClick = { }) {
//                    Icon(
//                        painter = painterResource(id = R.drawable.cast_24px),
//                        contentDescription = "Cast",
//                        tint = headerContentColor,
//                        modifier = Modifier.size(24.dp)
//                    )
//                }

                CastButton(
                    color = headerContentColor,
                    modifier = Modifier.padding(end = 8.dp)
                )

                IconButton(onClick = onShare) {
                    Icon(
                        painter = painterResource(id = R.drawable.share_24px),
                        contentDescription = "Share",
                        tint = headerContentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

//                // Menu (Configurações)
//                IconButton(onClick = { }) {
//                    Icon(
//                        Icons.Rounded.MoreVert,
//                        contentDescription = "Menu",
//                        tint = headerContentColor,
//                        modifier = Modifier.size(24.dp)
//                    )
//                }
            }
        }

        // ... (Bloco Mini Player e Capa Animada iguais ao anterior) ...
        if (expandProgress < 0.9f) {
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
                            text = uiState.title.ifEmpty { "Selecione..." },
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
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            "Play",
                            tint = miniContentColor
                        )
                    }
                    IconButton(onClick = onFastForward) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            "Prox",
                            tint = miniContentColor
                        )
                    }
                }
            }
        }
        // ... (Capa Animada) ...
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




    if (showChapters) {
        br.app.ide.ouvindoabiblia.ui.player.components.ChaptersSheet(
            chapters = uiState.chapters,
            currentIndex = uiState.currentChapterIndex,
            accentColor = backgroundColor,
            onChapterClick = { index ->
                onChapterSelect(index)
            },
            onDismiss = { showChapters = false }
        )
    }

    if (showSpeedSheet) {
        SpeedBottomSheet(
            currentSpeed = uiState.playbackSpeed,
            onSpeedSelected = { newSpeed -> onSetSpeed(newSpeed) },
            accentColor = backgroundColor,
            onDismiss = { showSpeedSheet = false }
        )
    }

    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            accentColor = backgroundColor,
            currentMinutes = uiState.activeSleepTimerMinutes,
            onTimeSelected = { minutes -> onSetSleepTimer(minutes) },
            onDismiss = { showSleepTimerSheet = false }
        )
    }
}

@Preview(name = "Full Player", heightDp = 800, widthDp = 360, showBackground = true)
@Composable
fun FullPreview() {
    SharedPlayerScreen(
        expandProgress = 1f,
        uiState = PlayerUiState(
            title = "Zacarias",
            subtitle = "Capítulo 1",
            imageUrl = "",
            isPlaying = false
        ),
        backgroundColor = Color(0xFF8D7F60),
        onPlayPause = {},
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


@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (Int) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Parar áudio em", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                // Lista de opções
                val options = listOf(
                    5 to "5 minutos",
                    15 to "15 minutos",
                    30 to "30 minutos",
                    45 to "45 minutos",
                    60 to "1 hora",
                    0 to "Desativar" // 0 serve para cancelar
                )

                options.forEach { (minutes, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTimeSelected(minutes)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (minutes == 0) Icons.Rounded.Close else Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = if (minutes == 0) Color.Red else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}