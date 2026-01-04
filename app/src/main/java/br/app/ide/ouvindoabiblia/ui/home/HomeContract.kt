package br.app.ide.ouvindoabiblia.ui.home

// Estado da UI (LCE Pattern)
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Success(
        val continueListeningBook: BookSummary? = null, // Último ouvido
        val favoriteBooks: List<BookSummary> = emptyList(),
        val filteredBooks: List<BookSummary> = emptyList(), // Lista principal
        val selectedFilter: TestamentFilter = TestamentFilter.ALL
    ) : HomeUiState
}

// Modelos simplificados para UI (Mapeados do Domain)
data class BookSummary(
    val id: String, // ex: "genesis" ou "mateus"
    val title: String, // ex: "Gênesis"
    val imageUrl: String?, //
    val testament: String // "at" ou "nt"
)

enum class TestamentFilter { ALL, AT, NT }

// Intenções do Usuário (Events)
sealed interface HomeIntent {
    data class SelectFilter(val filter: TestamentFilter) : HomeIntent
    data class OpenBook(val bookId: String) : HomeIntent
    data object Retry : HomeIntent
}