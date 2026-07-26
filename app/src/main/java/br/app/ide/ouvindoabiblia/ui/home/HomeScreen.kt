package br.app.ide.ouvindoabiblia.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.ide.ouvindoabiblia.ui.theme.AppColors
import br.app.ide.ouvindoabiblia.ui.home.components.BookFilterBar
import br.app.ide.ouvindoabiblia.ui.home.components.BookGridItem
import br.app.ide.ouvindoabiblia.ui.home.components.ErrorScreen
import br.app.ide.ouvindoabiblia.ui.home.components.LoadingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass,
    onNavigateToBook: (Int, String, String) -> Unit,
    bottomContentPadding: Dp = 0.dp
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val uiState = state) {
        is HomeUiState.Loading -> LoadingScreen()
        is HomeUiState.Error -> ErrorScreen(uiState.message) {
            viewModel.handle(HomeIntent.Retry)
        }

        is HomeUiState.Success -> {
            HomeContent(
                state = uiState,
                onIntent = viewModel::handle,
                onNavigateToBook = onNavigateToBook,
                bottomContentPadding = bottomContentPadding
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    onIntent: (HomeIntent) -> Unit,
    onNavigateToBook: (Int, String, String) -> Unit,
    bottomContentPadding: Dp
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val resolvedBottomPadding = if (bottomContentPadding == 0.dp) {
        navBarPadding + 16.dp
    } else {
        bottomContentPadding
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = statusBarPadding + 30.dp,
            bottom = resolvedBottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HomeHeader()
        }

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
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

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

@Composable
private fun HomeHeader() {
    Text(
        text = "Ouvindo a Bíblia",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = AppColors.textPrimary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}