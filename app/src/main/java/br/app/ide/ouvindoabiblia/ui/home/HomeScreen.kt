package br.app.ide.ouvindoabiblia.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.ide.ouvindoabiblia.ui.components.BookFilterBar
import br.app.ide.ouvindoabiblia.ui.home.components.BookGridItem
import br.app.ide.ouvindoabiblia.ui.home.components.ContinueListeningCard
import br.app.ide.ouvindoabiblia.ui.home.components.ErrorScreen
import br.app.ide.ouvindoabiblia.ui.home.components.FavoriteBookItem
import br.app.ide.ouvindoabiblia.ui.home.components.LoadingScreen
import br.app.ide.ouvindoabiblia.ui.home.components.SectionHeader
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass,
    onNavigateToBook: (Int, String, String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val uiState = state) {
        is HomeUiState.Loading -> LoadingScreen()
        is HomeUiState.Error -> ErrorScreen(uiState.message) { viewModel.handle(HomeIntent.Retry) }
        is HomeUiState.Success -> {
            HomeContent(
                state = uiState,
                onIntent = viewModel::handle,
                onNavigateToBook = onNavigateToBook
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    onIntent: (HomeIntent) -> Unit,
    onNavigateToBook: (Int, String, String) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground), // <--- GARANTIA DE FUNDO CREME
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = statusBarPadding + 16.dp,
            bottom = navBarPadding + 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Título
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Ouvindo a Bíblia",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                // Cor Azul Escuro para contraste com o Creme
                color = br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // 1. Continuar Ouvindo
        state.continueListeningBook?.let { book ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SectionHeader(title = "Continuar Ouvindo")
                    ContinueListeningCard(
                        book = book,
                        onClick = { onNavigateToBook(book.id, book.title, book.imageUrl ?: "") }
                    )
                }
            }
        }

        // 2. Favoritos
        if (state.favoriteBooks.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SectionHeader(title = "Favoritos")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(items = state.favoriteBooks, key = { it.id }) { book ->
                            FavoriteBookItem(
                                book,
                                onClick = {
                                    onNavigateToBook(
                                        book.id,
                                        book.title,
                                        book.imageUrl ?: ""
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- 3. FILTROS
        item(span = { GridItemSpan(maxLineSpan) }) {

            BookFilterBar(
                selectedOption = state.selectedFilter.name,
                onOptionSelected = { filterString ->
                    val newFilter = when (filterString) {
                        "AT" -> TestamentFilter.AT
                        "NT" -> TestamentFilter.NT
                        else -> TestamentFilter.ALL
                    }
                    onIntent(HomeIntent.SelectFilter(newFilter))
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // 4. Header Lista
//        item(span = { GridItemSpan(maxLineSpan) }) {
//            SectionHeader(title = "Livros (${state.filteredBooks.size})")
//        }

        // 5. Grid Principal
        items(
            items = state.filteredBooks,
            key = { book -> book.id },
            contentType = { "book_grid_item" }
        ) { book ->
            BookGridItem(
                book = book,
                onClick = { onNavigateToBook(book.id, book.title, book.imageUrl ?: "") }
            )
        }
    }
}