import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.ide.ouvindoabiblia.ui.home.HomeIntent
import br.app.ide.ouvindoabiblia.ui.home.HomeUiState
import br.app.ide.ouvindoabiblia.ui.home.HomeViewModel

// feature/home/HomeScreen.kt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(), //
    windowSizeClass: WindowSizeClass, //
    onNavigateToBook: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle() // Lifecycle-aware collection

    Scaffold(
        topBar = { HomeTopBar() }
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
    // Lógica adaptativa para colunas (Celular vs Tablet)
    val columns = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        else -> 4 // Tablet/Desktop expandido
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
        // SEÇÃO 1: Continuar Ouvindo (Ocupa largura total)
        state.continueListeningBook?.let { book ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(title = "Continuar Ouvindo")
                ContinueListeningCard(
                    book = book,
                    onClick = { onNavigateToBook(book.id) }
                )
            }
        }

        // SEÇÃO 2: Favoritos (Lista Horizontal dentro do Grid vertical)
        if (state.favoriteBooks.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SectionHeader(title = "Favoritos")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(state.favoriteBooks) { book ->
                            FavoriteBookItem(book, onClick = { onNavigateToBook(book.id) })
                        }
                    }
                }
            }
        }

        // SEÇÃO 3: Filtros (Chips)
        item(span = { GridItemSpan(maxLineSpan) }) {
            TestamentFilterRow(
                selected = state.selectedFilter,
                onSelect = { onIntent(HomeIntent.SelectFilter(it)) }
            )
        }

        // SEÇÃO 4: Grid de Todos os Livros (Itens individuais do Grid)
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(title = "Livros (${state.filteredBooks.size})")
        }

        items(
            items = state.filteredBooks,
            key = { it.id } // Importante para performance do Lazy
        ) { book ->
            BookGridItem(
                book = book,
                onClick = { onNavigateToBook(book.id) }
            )
        }

        // Espaço extra no fim para não ficar atrás do futuro MiniPlayer
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}