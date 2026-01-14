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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import br.app.ide.ouvindoabiblia.ui.theme.isDark
import coil.compose.AsyncImage
import kotlin.math.roundToInt

@Composable
fun SharedPlayerScreen(
    expandProgress: Float,
    uiState: PlayerUiState,
    backgroundColor: Color,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onCollapse: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpen: () -> Unit
) {
    // BoxWithConstraints: Usado para calcular o tamanho da tela para a animação da capa
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current

        // --- CÁLCULOS DE GEOMETRIA ---

        // Usamos 'this.maxWidth' diretamente para evitar o aviso de "scope unused"
        val screenWidth = this.maxWidth

        // 1. Tamanho da Capa
        val miniImageSize = 48.dp
        val fullImageSize = (screenWidth * 0.85f) // 85% da largura da tela
        val currentImageSize = lerp(miniImageSize, fullImageSize, expandProgress)

        // 2. Posição X (Horizontal)
        val imageStartX = 16.dp
        val imageEndX = (screenWidth - fullImageSize) / 2 // Centralizado
        val currentImageX = lerp(imageStartX, imageEndX, expandProgress)

        // 3. Posição Y (Vertical)
        val imageStartY = 8.dp
        val imageEndY = 100.dp
        val currentImageY = lerp(imageStartY, imageEndY, expandProgress)

        // 4. Arredondamento dos Cantos
        val imageCorner = lerp(4.dp, 16.dp, expandProgress)

        // 5. Sombra da Capa (Dinâmica)
        val imageShadow = lerp(6.dp, 16.dp, expandProgress)

        // --- CORES DINÂMICAS ---
        val isBackgroundDark = backgroundColor.isDark()
        val miniContentColor = if (isBackgroundDark) Color.White else Color.Black

        // --- FUNDO ---
        // Camada do gradiente Full (aparece conforme expande)
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

        // --- CONTEÚDO FULL (TEXTOS E BOTÕES GRANDES) ---
        if (expandProgress > 0.1f) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp + fullImageSize + 32.dp)
                    .padding(horizontal = 24.dp)
                    .alpha(expandProgress),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                // Reutilizando a barra de progresso do PlayerScreen.kt
                PlayerProgressBar(
                    currentPosition = uiState.currentPosition,
                    duration = uiState.duration,
                    onSeek = onSeek
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Controles Principais
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botão Voltar (Usando SkipNext rotacionado 180 graus)
                    IconButton(onClick = onSkipPrev, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Voltar",
                            tint = Color.White,
                            modifier = Modifier
                                .size(32.dp)
                                .rotate(180f)
                        )
                    }

                    // Play/Pause Grande
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(72.dp),
                        onClick = onPlayPause
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Botão Avançar
                    IconButton(onClick = onSkipNext, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            "Avançar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // --- HEADER DA TELA FULL (SETA P/ BAIXO) ---
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
                    "Minimizar",
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
                Icon(Icons.Rounded.MoreVert, "Opções", tint = Color.White)
            }
        }

        // --- CONTEÚDO MINI (TEXTO E BOTÕES PEQUENOS) ---
        if (expandProgress < 0.9f) {
            val miniAlpha = 1f - (expandProgress * 3).coerceIn(0f, 1f)

            if (miniAlpha > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .alpha(miniAlpha)
                        .padding(start = 16.dp + 48.dp + 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.FavoriteBorder, "Favoritar", tint = miniContentColor)
                    }
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = miniContentColor
                        )
                    }

                    IconButton(onClick = onSkipNext) {
                        Icon(Icons.Rounded.SkipNext, "Próximo", tint = miniContentColor)
                    }
                }
            }
        }

        // --- CAPA DO ÁLBUM (COMPARTILHADA) ---
        // Desenhada por último para ficar em cima de tudo (z-index maior)
        Surface(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = with(density) { currentImageX.toPx().roundToInt() },
                        y = with(density) { currentImageY.toPx().roundToInt() }
                    )
                }
                .size(currentImageSize)
                // Se o player estiver pequeno, o clique na capa abre o player
                .clickable(enabled = expandProgress < 0.5f) { onOpen() }
                .shadow(
                    elevation = imageShadow,
                    shape = RoundedCornerShape(imageCorner),
                    spotColor = Color.Black,
                    ambientColor = Color.Black
                ),
            shape = RoundedCornerShape(imageCorner),
            color = Color.DarkGray,
            shadowElevation = 0.dp
        ) {
            if (uiState.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = uiState.imageUrl,
                    contentDescription = "Capa do Livro",
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