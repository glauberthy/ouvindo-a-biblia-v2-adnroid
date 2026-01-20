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
import androidx.compose.ui.tooling.preview.Preview
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
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val screenWidth = this.maxWidth

        // --- CÁLCULOS GEOMÉTRICOS (Retângulo Pequeno -> Retângulo Grande) ---

        // 1. Definições do MINI PLAYER (Livro Pequeno)
        // Altura do livro no mini player
        val miniHeight = 56.dp
        // Largura respeitando proporção de livro (0.7)
        val miniWidth = miniHeight * 0.7f

        // 2. Definições do FULL PLAYER (Livro Grande)
        // Largura ocupa 70% da tela
        val fullWidth = screenWidth * 0.55f
        // Altura respeitando a MESMA proporção
        val fullHeight = fullWidth / 0.7f

        // 3. Interpolação (Animação)
        val currentWidth = lerp(miniWidth, fullWidth, expandProgress)
        val currentHeight = lerp(miniHeight, fullHeight, expandProgress)

        // 4. Posição X (Horizontal)
        // No Mini: Encostado na esquerda (com margem)
        val imageStartX = 16.dp
        // No Full: Centralizado na tela
        val imageEndX = (screenWidth - fullWidth) / 2
        val currentX = lerp(imageStartX, imageEndX, expandProgress)

        // 5. Posição Y (Vertical)
        // No Mini: Centralizado verticalmente na barra de 64dp (aprox 4dp de topo)
        val imageStartY = 4.dp
        // No Full: Margem superior maior
        val imageEndY = 100.dp
        val currentY = lerp(imageStartY, imageEndY, expandProgress)

        // Arredondamento e Sombra
        val imageCorner = lerp(4.dp, 12.dp, expandProgress)
        val imageShadow = lerp(2.dp, 16.dp, expandProgress)

        // Cores e Tema
        val isBackgroundDark = backgroundColor.isDark()
        val headerContentColor = if (isBackgroundDark) Color.White else Color.Black
        val miniContentColor = if (isBackgroundDark) Color.White else Color.Black
        val playerControlsColor = Color.White
        val playerSecondaryColor = Color.LightGray

        // --- FUNDO COM DEGRADÊ ---
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

        // --- CONTEÚDO TELA CHEIA (Full Player) ---
        if (expandProgress > 0.1f) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = imageEndY + fullHeight + 24.dp)
                    .padding(horizontal = 24.dp)
                    .alpha(expandProgress),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = playerSecondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                PlayerProgressBar(
                    currentPosition = uiState.currentPosition,
                    duration = uiState.duration,
                    onSeek = onSeek
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 190.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSkipPrev, modifier = Modifier.size(56.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Voltar",
                            tint = playerControlsColor,
                            modifier = Modifier
                                .size(36.dp)
                                .rotate(180f)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(80.dp),
                        onClick = onPlayPause
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    IconButton(onClick = onSkipNext, modifier = Modifier.size(56.dp)) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            "Avançar",
                            tint = playerControlsColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // --- HEADER ---
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
                    tint = headerContentColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "TOCANDO AGORA",
                color = headerContentColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                letterSpacing = 2.sp
            )
            IconButton(onClick = {}, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    Icons.Rounded.MoreVert,
                    "Opções",
                    tint = headerContentColor
                )
            }
        }

        // --- MINI PLAYER ---
        if (expandProgress < 0.9f) {
            val miniAlpha = 1f - (expandProgress * 3).coerceIn(0f, 1f)

            if (miniAlpha > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .alpha(miniAlpha)
                        // Aumentei o padding esquerdo (start) para compensar a largura da imagem
                        // miniWidth é aprox 40dp + margem 16dp + espaçamento 12dp
                        .padding(start = 16.dp + miniWidth + 12.dp, end = 8.dp),
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

        // --- A CAPA (ANIMADA) ---
        Surface(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = with(density) { currentX.toPx().roundToInt() },
                        y = with(density) { currentY.toPx().roundToInt() }
                    )
                }
                .size(width = currentWidth, height = currentHeight)
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
                    contentScale = ContentScale.Crop, // Crop agora funciona perfeito pois o container já é retangular
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


// --- ÁREA DE PREVIEW (SÓ PARA DESENVOLVIMENTO) ---

@Preview(
    name = "Player Aberto (Full)",
    showBackground = true,
    heightDp = 800, // Simula altura de um celular
    widthDp = 360
)
@Composable
fun PlayerFullPreview() {
    // Dados Fakes só para testar o visual
    SharedPlayerScreen(
        expandProgress = 1f, // 1.0f = Aberto total
        uiState = PlayerUiState(
            title = "Zacarias",
            subtitle = "Capítulo 1",
            imageUrl = "", // Deixe vazio ou coloque uma URL fake
            isPlaying = true,
            duration = 240000L, // 4 minutos
            currentPosition = 60000L // 1 minuto
        ),
        backgroundColor = Color(0xFF8D7F60), // Uma cor de capa qualquer (Marrom/Dourado)
        onPlayPause = {},
        onSkipNext = {},
        onSkipPrev = {},
        onCollapse = {},
        onSeek = {},
        onOpen = {}
    )
}

@Preview(
    name = "Player Fechado (Mini)",
    showBackground = true,
    heightDp = 64, // Altura só da barra
    widthDp = 360
)
@Composable
fun PlayerMiniPreview() {
    SharedPlayerScreen(
        expandProgress = 0f, // 0.0f = Fechado (Mini player)
        uiState = PlayerUiState(
            title = "Zacarias",
            subtitle = "Capítulo 1",
            imageUrl = "",
            isPlaying = false
        ),
        backgroundColor = Color(0xFF8D7F60),
        onPlayPause = {},
        onSkipNext = {},
        onSkipPrev = {},
        onCollapse = {},
        onSeek = {},
        onOpen = {}
    )
}