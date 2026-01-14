package br.app.ide.ouvindoabiblia.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import coil.compose.AsyncImage

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onCollapse: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Fundo Gradiente Escuro (Estilo Spotify)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF424242), // Cinza escuro no topo
            Color(0xFF121212)  // Preto profundo embaixo
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. TOPO
            TopPlayerBar(
                headerTitle = "Tocando Agora",
                onCollapse = onCollapse
            )

            Spacer(modifier = Modifier.weight(1f))

            // 2. CAPA
            AlbumCover(imageUrl = uiState.imageUrl)

            Spacer(modifier = Modifier.height(32.dp))

            // 3. INFO
            TrackInfoSection(
                title = uiState.title,
                subtitle = uiState.subtitle
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4. PROGRESSO
            PlayerProgressBar(
                currentPosition = uiState.currentPosition,
                duration = uiState.duration,
                onSeek = { viewModel.seekTo(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. CONTROLES (Aqui estava o erro, agora corrigido com vetores)
            PlayerControls(
                isPlaying = uiState.isPlaying,
                isShuffleOn = uiState.isShuffleEnabled,
                repeatMode = uiState.repeatMode,
                onPlayPause = { viewModel.togglePlayPause() },
                onSkipNext = { viewModel.skipToNextChapter() },
                onSkipPrev = { viewModel.skipToPreviousChapter() },
                onShuffleToggle = { viewModel.toggleShuffle() },
                onRepeatToggle = { viewModel.toggleRepeat() }
            )

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TopPlayerBar(headerTitle: String, onCollapse: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCollapse) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Minimizar",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = headerTitle.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            letterSpacing = 2.sp
        )

        IconButton(onClick = { /* Menu */ }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opções",
                tint = Color.White
            )
        }
    }
}

@Composable
fun AlbumCover(imageUrl: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        if (imageUrl.isNotEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Capa do Livro",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // CORREÇÃO: Usando Vector MusicNote em vez de drawable antigo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }
}

@Composable
fun TrackInfoSection(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { /* Favoritar */ }) {
            Icon(
                imageVector = Icons.Rounded.FavoriteBorder,
                contentDescription = "Favoritar",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun PlayerProgressBar(currentPosition: Long, duration: Long, onSeek: (Long) -> Unit) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f

    Column {
        Slider(
            value = progress,
            onValueChange = { newProgress ->
                onSeek((newProgress * duration).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = formatTime(duration),
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun PlayerControls(
    isPlaying: Boolean,
    isShuffleOn: Boolean,
    repeatMode: Int,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Shuffle (Ícone Vetorial)
        IconButton(onClick = onShuffleToggle) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Aleatório",
                tint = if (isShuffleOn) Color.Green else Color.Gray
            )
        }

        // Previous (Ícone Vetorial)
        IconButton(onClick = onSkipPrev, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Voltar",
                tint = Color.White,
                modifier = Modifier.fillMaxSize()
            )
        }

        // PLAY / PAUSE (Botão Branco Grande Vetorial)
        Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.size(72.dp),
            onClick = onPlayPause
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Next (Ícone Vetorial)
        IconButton(onClick = onSkipNext, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Avançar",
                tint = Color.White,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Repeat (Ícone Vetorial)
        IconButton(onClick = onRepeatToggle) {
            val isRepeatOn = repeatMode != Player.REPEAT_MODE_OFF
            val icon =
                if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat

            Icon(
                imageVector = icon,
                contentDescription = "Repetir",
                tint = if (isRepeatOn) Color.Green else Color.Gray
            )
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
fun MiniPlayer(
    uiState: PlayerUiState,
    onPlayPause: () -> Unit,
    onOpen: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Surface APENAS Visual (Sem onClick aqui para não roubar eventos)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp), // Altura fixa da pílula
        color = Color(0xFFF5F5F5),
        shape = RectangleShape, // Shape controlado pelo pai
        shadowElevation = 0.dp // Sombra controlada pelo pai
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            // 2. Botão Play/Pause (Área de clique própria)
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 3. A ÁREA DE EXPANSÃO (Texto)
            // Aplicamos o clique de abrir APENAS aqui no meio.
            // O weight(1f) garante que ele ocupe todo o espaço entre os botões.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize() // Preenche a altura para facilitar o clique
                    .clickable { onOpen() }, // <--- AQUI ESTÁ A CORREÇÃO
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.title.ifEmpty { "Selecione um livro" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (uiState.subtitle.isNotEmpty()) {
                    Text(
                        text = uiState.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. Botões da Direita (Área de clique própria)
            IconButton(onClick = { /* Fav */ }) {
                Icon(
                    imageVector = Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = Color.Black
                )
            }

            IconButton(onClick = onSkipNext) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = null,
                    tint = Color.Black
                )
            }
        }
    }
}