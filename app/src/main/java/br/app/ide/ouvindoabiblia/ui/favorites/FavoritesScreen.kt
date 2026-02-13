package br.app.ide.ouvindoabiblia.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo
import br.app.ide.ouvindoabiblia.ui.theme.Accent
import br.app.ide.ouvindoabiblia.ui.theme.Accent2
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import coil.compose.AsyncImage

@Composable
fun FavoritesScreen(
    // Atualizado para receber o index do capítulo (0 a N)
    onPlayChapter: (String, String, String, Int) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Usamos a versão "Stateless" para permitir o uso do Preview no editor
    FavoritesScreenContent(
        uiState = uiState,
        onPlayChapter = onPlayChapter,
        onRemove = { viewModel.removeFromFavorites(it) }
    )
}

@Composable
fun FavoritesScreenContent(
    uiState: FavoritesUiState,
    onPlayChapter: (String, String, String, Int) -> Unit,
    onRemove: (Long) -> Unit
) {
    // LazyColumn agora é o container raiz para permitir o scroll do título
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2E9E4)), // Creme elegante
        contentPadding = PaddingValues(
            top = 80.dp,    // Espaço para passar por baixo da Status Bar
            bottom = 150.dp, // Espaço para não ser coberto pelo Mini Player
            start = 20.dp,
            end = 20.dp
        )
    ) {
        // O TÍTULO agora é um item da lista. Ele sobe junto com o scroll.
        item {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "Meus Favoritos",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color(0xFF22223B),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sua coleção particular de capítulos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4A4E69)
                )
            }
        }

        // CONTEÚDO DINÂMICO
        when (uiState) {
            is FavoritesUiState.Loading -> {
                item {
                    Box(Modifier.fillParentMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF22223B))
                    }
                }
            }

            is FavoritesUiState.Empty -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight(0.7f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyFavorites()
                    }
                }
            }

            is FavoritesUiState.Success -> {
                items(uiState.favorites, key = { it.chapter.id }) { item ->
                    FavoriteItemCard(
                        item = item,
                        onClick = {
                            // Calcula o index baseado no número do capítulo (Ex: Cap 1 -> Index 0)
                            val startIndex = (item.chapter.number - 1).coerceAtLeast(0)
                            onPlayChapter(
                                item.chapter.bookId,
                                item.bookName,
                                item.coverUrl ?: "",
                                startIndex
                            )
                        },
                        onRemove = { onRemove(item.chapter.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun FavoriteItemCard(item: ChapterWithBookInfo, onClick: () -> Unit, onRemove: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.bookName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF22223B)
                )
                Text(
                    text = "Capítulo ${item.chapter.number}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4A4E69)
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = Accent2
                )
            }
        }
    }
}

@Composable
fun EmptyFavorites() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFC9ADA7)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sua lista está vazia",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF4A4E69),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Marque capítulos como favoritos\npara ouvi-los novamente com facilidade.",
            color = Color(0xFF9A8C98),
            textAlign = TextAlign.Center
        )
    }
}

// --- PREVIEW PARA O EDITOR ---
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun FavoritesScreenPreview() {
    FavoritesScreenContent(
        uiState = FavoritesUiState.Success(emptyList()), // Pode simular Success com lista mockada
        onPlayChapter = { _, _, _, _ -> },
        onRemove = {}
    )
}