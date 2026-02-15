package br.app.ide.ouvindoabiblia.ui.favorites

// Importando sua paleta de cores
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import br.app.ide.ouvindoabiblia.ui.theme.Accent2
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray
import br.app.ide.ouvindoabiblia.ui.theme.RosyBeige
import br.app.ide.ouvindoabiblia.ui.theme.SlateBlue
import coil.compose.AsyncImage

@Composable
fun FavoritesScreen(
    onPlayChapter: (Int, String, String, Int) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    FavoritesScreenContent(
        uiState = uiState,
        onPlayChapter = onPlayChapter,
        onRemove = { viewModel.removeFromFavorites(it) }
    )
}

@Composable
fun FavoritesScreenContent(
    uiState: FavoritesUiState,
    onPlayChapter: (Int, String, String, Int) -> Unit,
    onRemove: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground), // Fundo papel antigo
        contentPadding = PaddingValues(top = 80.dp, bottom = 150.dp, start = 20.dp, end = 20.dp)
    ) {
        // 1. TÍTULO
        item {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "Meus Favoritos",
                    style = MaterialTheme.typography.headlineLarge,
                    color = DeepBlueDark, // Texto principal escuro
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sua coleção particular de capítulos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateBlue // Texto secundário suave
                )
            }
        }

        // 2. CONTROLE DE ESTADOS
        when (uiState) {
            is FavoritesUiState.Loading -> {
                item {
                    Box(Modifier.fillParentMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = DeepBlueDark)
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
                val grouped = uiState.favorites.groupBy { it.bookName }

                grouped.forEach { (_, chapters) ->
                    item {
                        val info = chapters.first()

                        // CARD UNIFICADO POR LIVRO
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(20.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Cabeçalho do Livro
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = info.coverUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(90.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(RosyBeige)
                                    )

                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        // Badge AT/NT curto e elegante
                                        Surface(
                                            color = if (info.testament == "at") RosyBeige else LavenderGray,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (info.testament == "at") "AT" else "NT",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 2.dp
                                                )
                                            )
                                        }

                                        Text(
                                            text = info.bookName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepBlueDark
                                        )
                                        Text(
                                            text = "${info.totalChapters} capítulos no total",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SlateBlue.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = CreamBackground, thickness = 1.dp)

                                // Lista de capítulos favoritados deste livro
                                chapters.forEach { item ->
                                    ChapterListItem(
                                        number = item.chapter.number,
                                        onClick = {
                                            val index = (item.chapter.number - 1).coerceAtLeast(0)
                                            onPlayChapter(
                                                item.chapter.bookId,
                                                item.bookName,
                                                item.coverUrl ?: "",
                                                index
                                            )
                                        },
                                        onRemove = { onRemove(item.chapter.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterListItem(number: Int, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = SlateBlue // Ícone discreto
            )
            Text(
                text = "Capítulo $number",
                style = MaterialTheme.typography.bodyLarge,
                color = DeepBlueDark,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Accent2,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmptyFavorites() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = RosyBeige // Cor sutil para o estado vazio
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sua lista está vazia",
            style = MaterialTheme.typography.titleMedium,
            color = SlateBlue,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Marque capítulos como favoritos\npara ouvi-los novamente com facilidade.",
            color = LavenderGray,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}