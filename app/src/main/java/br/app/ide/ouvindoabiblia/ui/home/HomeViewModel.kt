package br.app.ide.ouvindoabiblia.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _selectedFilter = MutableStateFlow(TestamentFilter.ALL)

    // A mágica acontece aqui: flowOn(Dispatchers.Default)
    val uiState: StateFlow<HomeUiState> = combine(
        _isLoading,
        _error,
        _selectedFilter,
        repository.getBooks()
    ) { isLoading, error, filter, books ->

        // Regra de Ouro: Se tem dados, ignora o loading e mostra o conteúdo
        if (books.isNotEmpty()) {

            // Processamento Pesado (Sort/Map/Filter) agora roda em Background
            val bookSummaries = books
                .sortedBy { it.numericId }
                .map {
                    BookSummary(
                        id = it.bookId,
                        title = it.name,
                        imageUrl = if (it.imageUrl.isNullOrBlank()) null else it.imageUrl,
                        testament = it.testament
                    )
                }

            val filtered = when (filter) {
                TestamentFilter.ALL -> bookSummaries
                TestamentFilter.AT -> bookSummaries.filter { it.testament == "at" }
                TestamentFilter.NT -> bookSummaries.filter { it.testament == "nt" }
            }

            HomeUiState.Success(
                filteredBooks = filtered,
                selectedFilter = filter
            )
        } else if (isLoading) {
            HomeUiState.Loading
        } else if (error != null) {
            HomeUiState.Error(error)
        } else {
            // Estado inicial ou vazio
            HomeUiState.Loading
        }
    }
        .flowOn(Dispatchers.Default) // <--- CORREÇÃO CRÍTICA: Tira o peso da Main Thread
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading
        )

    init {
        syncData()
    }

    fun handle(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SelectFilter -> _selectedFilter.value = intent.filter
            is HomeIntent.Retry -> syncData()
            is HomeIntent.OpenBook -> { /* Navegação tratada na UI */
            }
        }
    }

    private fun syncData() {
        viewModelScope.launch {
            // OTIMIZAÇÃO: Verifica se o banco JÁ tem dados antes de mostrar loading.
            // firstOrNull() pega o valor atual do Flow do Room rapidinho.
            val currentBooks = repository.getBooks().firstOrNull()
            val hasData = !currentBooks.isNullOrEmpty()

            // Só mostra loading visual se a tela estiver vazia
            if (!hasData) {
                _isLoading.value = true
            }

            _error.value = null

            try {
                // Chama o Repository (que agora tem o Smart Sync)
                // Se já estiver atualizado, isso retorna quase instantaneamente.
                val result = repository.syncBibleData()

                result.onFailure { exception ->
                    // Só mostra erro na tela se não tivermos dados cacheado para mostrar
                    if (!hasData) {
                        _error.value = "Erro de conexão: ${exception.localizedMessage}"
                    } else {
                        // Se tem dados, apenas loga o erro de sync silenciosamente
                        Log.w("HomeViewModel", "Falha no sync em background: ${exception.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Crash Crítico: ${e.message}", e)
                if (!hasData) _error.value = "Erro crítico: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}