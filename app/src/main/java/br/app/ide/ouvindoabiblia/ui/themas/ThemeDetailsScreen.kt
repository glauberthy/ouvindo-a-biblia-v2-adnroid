package br.app.ide.ouvindoabiblia.ui.themas

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.ide.ouvindoabiblia.data.local.model.MomentWithAudio
import br.app.ide.ouvindoabiblia.ui.home.components.ErrorScreen
import br.app.ide.ouvindoabiblia.ui.home.components.LoadingScreen
import br.app.ide.ouvindoabiblia.ui.player.PlayerViewModel
import br.app.ide.ouvindoabiblia.ui.theme.Accent
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDetailsScreen(
    viewModel: ThemeDetailsViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(), // Injeção do estado do player
    onBackClick: () -> Unit,
    onPlayTheme: (List<MomentWithAudio>, Int, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val themeTitle = viewModel.themeTitle

    // Identifica qual índice está tocando se o tema ativo for igual ao desta tela
    val playingIndex = if (playerState.title == themeTitle) {
        playerState.currentChapterIndex
    } else {
        -1
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = themeTitle,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlueDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = DeepBlueDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CreamBackground // Mantém a imersão na paleta
                )
            )
        },
        containerColor = CreamBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is ThemeDetailsUiState.Loading -> LoadingScreen()
                is ThemeDetailsUiState.Error -> ErrorScreen(state.message) { viewModel.loadMoments() }
                is ThemeDetailsUiState.Success -> {
                    MomentsList(
                        moments = state.moments,
                        themeTitle = themeTitle,
                        playingIndex = playingIndex, // Novo parâmetro
                        onPlayTheme = onPlayTheme
                    )
                }
            }
        }
    }
}

@Composable
private fun MomentsList(
    moments: List<MomentWithAudio>,
    themeTitle: String,
    playingIndex: Int, // Novo parâmetro
    onPlayTheme: (List<MomentWithAudio>, Int, String) -> Unit
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = navBarPadding + 48.dp // Espaço para o player flutuante!
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Versículos Selecionados",
                style = MaterialTheme.typography.titleMedium,
                color = LavenderGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        itemsIndexed(moments, key = { _, item -> item.moment.id }) { index, momentAudio ->
            val isPlaying = index == playingIndex

            MomentListItem(
                index = index + 1,
                item = momentAudio,
                isPlaying = isPlaying, // Informa se este item específico está tocando
                onClick = { onPlayTheme(moments, index, themeTitle) }
            )
        }
    }
}

@Composable
fun MomentListItem(
    index: Int,
    item: MomentWithAudio,
    isPlaying: Boolean,
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
        border = if (isPlaying) androidx.compose.foundation.BorderStroke(2.dp, Accent) else null,
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
                    .background(if (isPlaying) Accent else DeepBlueDark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    // Cor do texto do número
                    color = if (isPlaying) DeepBlueDark else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos (Mantidos sem alteração de cor de fundo)
            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    text = "${item.bookName} ${item.moment.reference}",
//                    style = MaterialTheme.typography.labelMedium,
//                    color = LavenderGray
//                )
                Text(
                    text = item.moment.reference,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlueDark
                )
                Text(
                    text = item.moment.title,
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
                    .background(if (isPlaying) Accent else DeepBlueDark.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) {
                        Icons.Default.Replay
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = if (isPlaying) "Reiniciar Versículo" else "Ouvir Versículo",
                    tint = DeepBlueDark
                )
            }
        }
    }
}