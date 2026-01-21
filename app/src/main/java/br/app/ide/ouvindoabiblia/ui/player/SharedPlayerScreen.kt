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
    onCollapse: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpen: () -> Unit
) {
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
                            style = MaterialTheme.typography.titleLarge, // Fonte média, não enorme
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

                    // Botão Favorito (Vazado e fino)
                    IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                        Icon(
//                            imageVector = Icons.Outlined.FavoriteBorder,
                            painter = painterResource(id = R.drawable.favorite_24px),
                            contentDescription = "Favoritar",
                            tint = playerControlsColor,
                            modifier = Modifier.size(32.dp) // Ícone pequeno
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

                Spacer(modifier = Modifier.height(24.dp))

                // === ROW 3: CONTROLES DE PLAYBACK (Harmonizados e Finos) ===
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. VELOCIDADE
                    Box(
                        modifier = Modifier
                            .size(32.dp) // Área de toque menor
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1x",
                            style = MaterialTheme.typography.bodyMedium,
                            color = playerControlsColor,
                            fontSize = 24.sp
                        )
                    }

                    // 2. VOLTAR 10s (Ícone Circular)
                    IconButton(onClick = onRewind, modifier = Modifier.size(48.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = R.drawable.replay_10_24px),
                                contentDescription = "-10s",
                                tint = playerControlsColor,
                                modifier = Modifier.size(40.dp)
                            )
//                            Text("10", style = MaterialTheme.typography.labelSmall, color = playerControlsColor, fontSize = 10.sp)
                        }
                    }

                    // 3. PLAY/PAUSE (O único grande, mas nem tanto)
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(56.dp), // Reduzido de 64/80
                        onClick = onPlayPause
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }


                    IconButton(onClick = onFastForward, modifier = Modifier.size(48.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
//                                imageVector = Icons.Outlined.Forward10,
                                painter = painterResource(id = R.drawable.forward_10_24px),
                                contentDescription = "15s",
                                tint = playerControlsColor,
                                modifier = Modifier
                                    .size(40.dp)
                            )
//                            Text("15", style = MaterialTheme.typography.labelSmall, color = playerControlsColor, fontSize = 10.sp)
                        }
                    }

                    // 5. SLEEP (Lua)
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
//                            imageVector = R.drawable.time_stop,
                            painter = painterResource(id = R.drawable.mode_night_24px),
                            contentDescription = "Sleep",
                            tint = playerControlsColor, // Pode colocar .copy(alpha = 0.8f) se achar muito branco
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Espaçador fixo para aproximar rodapé
                Spacer(modifier = Modifier.height(48.dp))

                // === ROW 4: AÇÕES DO SISTEMA (Pequenos e Discretos) ===
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(48.dp) // Mais juntinhos
                    ) {
                        IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.cast_24px),
                                contentDescription = "Lista",
                                tint = playerControlsColor.copy(alpha = 1.0f),
                                modifier = Modifier.size(32.dp) // <--- BEM PEQUENO
                            )
                        }
                        IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.share_24px),
                                contentDescription = "Share",
                                tint = playerControlsColor.copy(alpha = 1.0f),
                                modifier = Modifier.size(32.dp)
                            )
                        }


                    }

                    IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.menu_24px),
                            contentDescription = "Cast",
                            tint = playerControlsColor.copy(alpha = 1.0f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // --- HEADER (Minimizar) ---
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
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Rounded.FavoriteBorder,
                            "Fav",
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
}

@Preview(name = "Full Player", heightDp = 800, widthDp = 360, showBackground = true)
@Composable
fun FullPreview() {
    SharedPlayerScreen(
        1f,
        PlayerUiState("Zacarias", "Capítulo 1", "", false),
        Color(0xFF8D7F60),
        {},
        {},
        {},
        {},
        {},
        {})
}