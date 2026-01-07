package br.app.ide.ouvindoabiblia.ui.home

// Estado da UI seguindo o padrão LCE (Loading, Content, Error)
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Success(
        val continueListeningBook: BookSummary? = null,
        val favoriteBooks: List<BookSummary> = emptyList(),
        val filteredBooks: List<BookSummary> = emptyList(),
        val selectedFilter: TestamentFilter = TestamentFilter.ALL
    ) : HomeUiState
}

data class BookSummary(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val testament: String
)

enum class TestamentFilter { ALL, AT, NT }

sealed interface HomeIntent {
    data class SelectFilter(val filter: TestamentFilter) : HomeIntent
    data class OpenBook(val bookId: String) : HomeIntent
    data object Retry : HomeIntent
}