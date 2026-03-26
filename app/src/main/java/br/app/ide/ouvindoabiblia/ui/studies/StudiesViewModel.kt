package br.app.ide.ouvindoabiblia.ui.studies

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
class StudiesViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StudiesUiState> = combine(
        _isLoading,
        _error,
        repository.getStudiesWithLessons()
    ) { isLoading, error, studies ->
        if (studies.isNotEmpty()) {
            StudiesUiState.Success(studies = studies)
        } else if (isLoading) {
            StudiesUiState.Loading
        } else if (error != null) {
            StudiesUiState.Error(error)
        } else {
            StudiesUiState.Error("Nenhum estudo encontrado. Verifique sua conexão e tente novamente.")
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudiesUiState.Loading
        )

    init {
        syncData()
    }

    fun handle(intent: StudiesIntent) {
        when (intent) {
            is StudiesIntent.Retry -> syncData()
            is StudiesIntent.SelectStudy -> { /* Navegação pela UI */
            }
        }
    }

    private fun syncData() {
        viewModelScope.launch {
            val currentStudies = repository.getStudiesWithLessons().firstOrNull()
            if (currentStudies.isNullOrEmpty()) {
                _isLoading.value = true
            }

            _error.value = null

            try {
                repository.syncStudies()
            } catch (exception: Exception) {
                if (currentStudies.isNullOrEmpty()) {
                    _error.value = "Erro ao carregar estudos: ${exception.localizedMessage}"
                } else {
                    Log.w("StudiesViewModel", "Falha no sync silencioso: ${exception.message}")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}