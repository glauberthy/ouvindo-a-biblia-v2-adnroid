package br.app.ide.ouvindoabiblia.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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

@OptIn(ExperimentalMaterial3Api::class) // Resolve o erro de API experimental
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
                    windowSizeClass = windowSizeClass,
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
    windowSizeClass: WindowSizeClass,
    onIntent: (HomeIntent) -> Unit,
    onNavigateToBook: (String) -> Unit
) {
    val columns = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        else -> 4
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        state.continueListeningBook?.let { book ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(title = "Continuar Ouvindo")
                ContinueListeningCard(book = book, onClick = { onNavigateToBook(book.id) })
            }
        }

        if (state.favoriteBooks.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SectionHeader(title = "Favoritos")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Correção do escopo: items da LazyRow
                        items(state.favoriteBooks) { book ->
                            FavoriteBookItem(book, onClick = { onNavigateToBook(book.id) })
                        }
                    }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            TestamentFilterRow(
                selected = state.selectedFilter,
                onSelect = { onIntent(HomeIntent.SelectFilter(it)) })
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(title = "Livros (${state.filteredBooks.size})")
        }

        items(items = state.filteredBooks, key = { it.id }) { book ->
            BookGridItem(book = book, onClick = { onNavigateToBook(book.id) })
        }
    }
}