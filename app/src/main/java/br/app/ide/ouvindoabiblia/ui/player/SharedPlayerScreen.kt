package br.app.ide.ouvindoabiblia.ui.player

// Importante: Importe a função isDark que criamos
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.app.ide.ouvindoabiblia.ui.theme.isDark
import coil.compose.AsyncImage
import kotlin.math.roundToInt

// Função utilitária para interpolação linear
fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

@Composable
fun SharedPlayerScreen(
    expandProgress: Float, // 0f = Mini, 1f = Full
    uiState: PlayerUiState,
    backgroundColor: Color, // <--- O PARÂMETRO QUE FALTAVA
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onCollapse: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpen: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val maxWidth = maxWidth
        val density = LocalDensity.current

        // --- CÁLCULOS DE GEOMETRIA ---

        // 1. Capa do Álbum
        val miniImageSize = 48.dp
        val fullImageSize = (maxWidth * 0.85f)
        val currentImageSize =
            androidx.compose.ui.unit.lerp(miniImageSize, fullImageSize, expandProgress)

        // Posição X da Capa
        val imageStartX = 16.dp
        val imageEndX = (maxWidth - fullImageSize) / 2
        val currentImageX = androidx.compose.ui.unit.lerp(imageStartX, imageEndX, expandProgress)

        // Posição Y da Capa
        val imageStartY = 8.dp
        val imageEndY = 100.dp
        val currentImageY = androidx.compose.ui.unit.lerp(imageStartY, imageEndY, expandProgress)

        // Arredondamento
        val imageCorner = androidx.compose.ui.unit.lerp(4.dp, 16.dp, expandProgress)

        // --- CORES DINÂMICAS ---
        // Se o fundo for escuro, texto branco. Se claro, texto preto.
        val isBackgroundDark = backgroundColor.isDark()
        val miniContentColor = if (isBackgroundDark) Color.White else Color.Black

        // 2. Fundo (Background)

        // Fundo Gradiente (aparece conforme expande no modo Full)
        // Começa com a cor da capa no topo e vai escurecendo para preto
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(expandProgress)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(backgroundColor, Color(0xFF121212))
                    )
                )
        )

        // --- CONTEÚDO ---

        // 1. ÁREA DE TEXTO E CONTROLES FULL
        if (expandProgress > 0.1f) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp + fullImageSize + 32.dp)
                    .padding(horizontal = 24.dp)
                    .alpha(expandProgress),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título Full
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = uiState.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Barra de Progresso
                PlayerProgressBar(
                    currentPosition = uiState.currentPosition,
                    duration = uiState.duration,
                    onSeek = onSeek
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Controles Full
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botão Previous
                    IconButton(onClick = onSkipPrev, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(32.dp)
                                .rotate(180f)
                        )
                    }

                    // Botão Play/Pause Grande
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(72.dp),
                        onClick = onPlayPause
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Botão Next
                    IconButton(onClick = onSkipNext, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // 2. HEADER DA TELA FULL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(top = 32.dp, start = 16.dp, end = 16.dp)
                .alpha(expandProgress)
        ) {
            IconButton(onClick = onCollapse, modifier = Modifier.align(Alignment.TopStart)) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Minimizar",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "TOCANDO AGORA",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                letterSpacing = 2.sp
            )
            IconButton(onClick = {}, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Opções", tint = Color.White)
            }
        }

        // 3. ELEMENTOS DO MINI PLAYER
        if (expandProgress < 0.9f) {
            val miniAlpha = 1f - (expandProgress * 3).coerceIn(0f, 1f)

            if (miniAlpha > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .alpha(miniAlpha)
                        .padding(start = 16.dp + 48.dp + 12.dp)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Texto Mini (Clicável)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onOpen() },
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.title.ifEmpty { "Selecione..." },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = miniContentColor, // USA A COR DINÂMICA AQUI
                            maxLines = 1
                        )
                        Text(
                            text = uiState.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = miniContentColor.copy(alpha = 0.7f), // USA A COR DINÂMICA AQUI
                            maxLines = 1
                        )
                    }

                    // Controles Mini
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = miniContentColor // USA A COR DINÂMICA AQUI
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.FavoriteBorder, null, tint = miniContentColor)
                    }
                    IconButton(onClick = onSkipNext) {
                        Icon(Icons.Rounded.SkipNext, null, tint = miniContentColor)
                    }
                }
            }
        }

        // 4. CAPA DO ÁLBUM
        Surface(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = with(density) { currentImageX.toPx().roundToInt() },
                        y = with(density) { currentImageY.toPx().roundToInt() }
                    )
                }
                .size(currentImageSize)
                .clickable(enabled = expandProgress < 0.5f) { onOpen() },
            shape = RoundedCornerShape(imageCorner),
            shadowElevation = if (expandProgress < 0.1f) 0.dp else 10.dp,
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
}