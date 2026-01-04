package br.app.ide.ouvindoabiblia.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.ide.ouvindoabiblia.data.local.entity.BookEntity
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado da UI (Simples e Imutável)
data class HomeUiState(
    val isLoading: Boolean = true,
    val books: List<BookEntity> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    // Gerenciamento de estado reativo
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    // Combina o fluxo do banco de dados com o estado de loading/erro
    val uiState: StateFlow<HomeUiState> = combine(
        _isLoading,
        _error,
        repository.getBooks()
    ) { isLoading, error, books ->
        HomeUiState(
            isLoading = isLoading && books.isEmpty(), // Só mostra loading se não tiver dados em cache
            books = books,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    init {
        syncData()
    }

    private fun syncData() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.syncBibleData()

            result.onFailure { exception ->
                _error.value = "Erro ao atualizar: ${exception.localizedMessage}"
            }

            _isLoading.value = false
        }
    }
}