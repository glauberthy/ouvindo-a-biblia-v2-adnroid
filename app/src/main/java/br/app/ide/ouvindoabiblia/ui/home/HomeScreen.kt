package br.app.ide.ouvindoabiblia.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.ide.ouvindoabiblia.ui.home.components.BookGridItem
import br.app.ide.ouvindoabiblia.ui.home.components.ContinueListeningCard
import br.app.ide.ouvindoabiblia.ui.home.components.ErrorScreen
import br.app.ide.ouvindoabiblia.ui.home.components.FavoriteBookItem
import br.app.ide.ouvindoabiblia.ui.home.components.LoadingScreen
import br.app.ide.ouvindoabiblia.ui.home.components.SectionHeader
import br.app.ide.ouvindoabiblia.ui.home.components.TestamentFilterRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass,
    onNavigateToBook: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Ouvindo a Bíblia") }) }
    ) { padding ->
        when (val uiState = state) {
            is HomeUiState.Loading -> LoadingScreen()
            is HomeUiState.Error -> ErrorScreen(uiState.message) { viewModel.handle(HomeIntent.Retry) }
            is HomeUiState.Success -> {
                HomeContent(
                    state = uiState,
                    padding = padding,
                    onIntent = viewModel::handle,
                    onNavigateToBook = onNavigateToBook
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    padding: PaddingValues,
    // Removi windowSizeClass daqui pois decidimos fixar em 3 colunas para visual de capa de álbum
    onIntent: (HomeIntent) -> Unit,
    onNavigateToBook: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3), // 3 Colunas fica perfeito para capas quadradas
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(padding)
    ) {
        // 1. Sessão Continuar Ouvindo
        state.continueListeningBook?.let { book ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SectionHeader(title = "Continuar Ouvindo")
                    ContinueListeningCard(book = book, onClick = { onNavigateToBook(book.id) })
                }
            }
        }

        // 2. Sessão Favoritos
        if (state.favoriteBooks.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SectionHeader(title = "Favoritos")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp) // Respiro visual
                    ) {
                        // OTIMIZAÇÃO 1: Adicionei key para a lista horizontal também!
                        items(state.favoriteBooks, key = { it.id }) { book ->
                            FavoriteBookItem(book, onClick = { onNavigateToBook(book.id) })
                        }
                    }
                }
            }
        }

        // 3. Filtros
        item(span = { GridItemSpan(maxLineSpan) }) {
            TestamentFilterRow(
                selected = state.selectedFilter,
                onSelect = { onIntent(HomeIntent.SelectFilter(it)) }
            )
        }

        // 4. Cabeçalho da Lista Principal
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(title = "Livros (${state.filteredBooks.size})")
        }

        // 5. Grid de Livros (A Parte Pesada)
        items(
            items = state.filteredBooks,
            key = { it.id }, // Já estava aqui, excelente!

            // OTIMIZAÇÃO 2: contentType
            // Isso diz ao Compose: "Todos esses itens usam o mesmo layout".
            // A renderização fica muito mais inteligente.
            contentType = { "book_grid_item" }
        ) { book ->
            BookGridItem(book = book, onClick = { onNavigateToBook(book.id) })
        }
    }
}