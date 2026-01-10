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
    // CORREÇÃO: Agora aceita 3 parâmetros (ID, Nome, Capa)
    onNavigateToBook: (String, String, String) -> Unit
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
    onIntent: (HomeIntent) -> Unit,
    // CORREÇÃO: Atualizado aqui também
    onNavigateToBook: (String, String, String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
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
                    ContinueListeningCard(
                        book = book,
                        // Passando a capa aqui também
                        onClick = { onNavigateToBook(book.id, book.title, book.imageUrl ?: "") })
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
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(state.favoriteBooks, key = { it.id }) { book ->
                            FavoriteBookItem(
                                book,
                                // Passando a capa aqui também
                                onClick = {
                                    onNavigateToBook(
                                        book.id,
                                        book.title,
                                        book.imageUrl ?: ""
                                    )
                                })
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

        // 5. Grid de Livros
        items(
            items = state.filteredBooks,
            key = { it.id },
            contentType = { "book_grid_item" }
        ) { book ->
            BookGridItem(
                book = book,
                // CORREÇÃO PRINCIPAL: Passando ID, Título e Capa
                onClick = { onNavigateToBook(book.id, book.title, book.imageUrl ?: "") }
            )
        }
    }
}