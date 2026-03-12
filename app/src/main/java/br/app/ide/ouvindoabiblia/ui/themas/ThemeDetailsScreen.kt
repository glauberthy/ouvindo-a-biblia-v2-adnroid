package br.app.ide.ouvindoabiblia.ui.themes

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
import br.app.ide.ouvindoabiblia.ui.themas.ThemeDetailsUiState
import br.app.ide.ouvindoabiblia.ui.themas.ThemeDetailsViewModel
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDetailsScreen(
    viewModel: ThemeDetailsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    // SINALIZAÇÃO: Vamos passar o objeto inteiro para que a navegação saiba tudo sobre o áudio e o tempo
    onPlayTheme: (moments: List<MomentWithAudio>, startIndex: Int, themeTitle: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeTitle = viewModel.themeTitle

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
            MomentListItem(
                index = index + 1,
                item = momentAudio,
                onClick = {
                    onPlayTheme(moments, index, themeTitle)
                }
            )
        }
    }
}

@Composable
fun MomentListItem(
    index: Int,
    item: MomentWithAudio,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Número / Índice circular
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos (Referência e Nome)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.bookName} ${item.moment.reference}", // Ex: Gênesis 1
                    style = MaterialTheme.typography.labelMedium,
                    color = LavenderGray
                )
                Text(
                    text = item.moment.reference, // Ex: Versículos 1-5
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlueDark
                )
                Text(
                    text = item.moment.title, // Ex: A Criação do Mundo
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepBlueDark.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Ícone de Play
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DeepBlueDark.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Ouvir Versículo",
                    tint = DeepBlueDark
                )
            }
        }
    }
}