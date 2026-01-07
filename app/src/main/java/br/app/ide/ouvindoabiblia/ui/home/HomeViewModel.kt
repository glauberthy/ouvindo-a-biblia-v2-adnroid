package br.app.ide.ouvindoabiblia.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    // Começa false para não travar a UI se o banco já tiver dados
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _selectedFilter = MutableStateFlow(TestamentFilter.ALL)

    val uiState: StateFlow<HomeUiState> = combine(
        _isLoading,
        _error,
        _selectedFilter,
        repository.getBooks()
    ) { isLoading, error, filter, books ->

        // Log para debug no Motorola
        Log.d(
            "HomeViewModel",
            "State Update -> Loading: $isLoading, Books: ${books.size}, Error: $error"
        )

        if (error != null && books.isEmpty()) {
            HomeUiState.Error(error)
        } else if (isLoading && books.isEmpty()) {
            HomeUiState.Loading
        } else {
            val bookSummaries = books.map {
                BookSummary(it.bookId, it.name, it.imageUrl, it.testament)
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
        }
    }.stateIn(
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
            is HomeIntent.OpenBook -> { /* TODO */
            }
        }
    }

    private fun syncData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // Limpa erro anterior

            try {
                Log.d("HomeViewModel", "Iniciando Sincronização...")
                val result = repository.syncBibleData()

                result.onSuccess {
                    Log.d("HomeViewModel", "Sucesso na Sincronização!")
                }

                result.onFailure { exception ->
                    Log.e("HomeViewModel", "Falha: ${exception.message}", exception)
                    _error.value = "Erro de conexão: ${exception.localizedMessage}"
                }
            } catch (e: Exception) {
                // Captura crashes inesperados na Coroutine
                Log.e("HomeViewModel", "Crash Crítico: ${e.message}", e)
                _error.value = "Erro crítico: ${e.message}"
            } finally {
                // GARANTE QUE O LOADING SUMA SEMPRE
                _isLoading.value = false
                Log.d("HomeViewModel", "Loading definido para false")
            }
        }
    }
}