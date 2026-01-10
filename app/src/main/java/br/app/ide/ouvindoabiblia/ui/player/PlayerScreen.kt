package br.app.ide.ouvindoabiblia.ui.player

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    bookTitle: String,
    chapterNumber: Int, // Esse valor agora é ignorado, pois vem do ViewModel
    coverUrl: String,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.background
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Topo: Botão Minimizar
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimizar",
                    Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Capa (Agora usamos a do state, se disponível, senão a da navegação)
        Card(
            modifier = Modifier
                .size(300.dp)
                .shadow(16.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            AsyncImage(
                // Prioriza a imagem do capítulo atual (playlist), se não tiver, usa a da navegação
                model = state.imageUrl.ifEmpty { coverUrl },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- MUDANÇA AQUI: Título e Subtítulo Dinâmicos ---

        // Nome do Livro (Ex: Gênesis)
        Text(
            text = state.title.ifEmpty { bookTitle },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // Número do Capítulo (Ex: Capítulo 1)
        // Antes usava "Capítulo $chapterNumber" (que era fixo 0)
        // Agora usa state.subtitle que muda sozinho
        Text(
            text = state.subtitle.ifEmpty { "Carregando..." },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // Slider
        Column {
            Slider(
                value = state.progress,
                onValueChange = { viewModel.seekTo((it * state.duration).toLong()) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(state.currentPosition), style = MaterialTheme.typography.bodySmall)
                Text(formatTime(state.duration), style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Controles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.skipBack() }, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.FastRewind,
                    contentDescription = "-10s",
                    modifier = Modifier.size(32.dp)
                )
            }

            FilledIconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(80.dp)
            ) {
                if (state.isBuffering) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                } else {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            IconButton(onClick = { viewModel.skipForward() }, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.FastForward,
                    contentDescription = "+10s",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@SuppressLint("DefaultLocale")
private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}