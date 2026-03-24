package br.app.ide.ouvindoabiblia.ui.themas

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
class ThemesViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Observa o banco de dados e o estado de carregamento/erro simultaneamente
    val uiState: StateFlow<ThemesUiState> = combine(
        _isLoading,
        _error,
        repository.getThemes()
    ) { isLoading, error, themes ->
        if (themes.isNotEmpty()) {
            ThemesUiState.Success(themes = themes)
        } else if (isLoading) {
            ThemesUiState.Loading
        } else if (error != null) {
            ThemesUiState.Error(error)
        } else {
            ThemesUiState.Error("Nenhum tema encontrado. Verifique sua conexão e tente novamente.")
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemesUiState.Loading
        )

    init {
        syncData()
    }

    fun handle(intent: ThemesIntent) {
        when (intent) {
            is ThemesIntent.Retry -> syncData()
            is ThemesIntent.SelectTheme -> { /* Navegação será tratada na Screen */
            }
        }
    }

    private fun syncData() {
        viewModelScope.launch {
            // Verifica se já temos dados cacheados para evitar loading desnecessário
            val currentThemes = repository.getThemes().firstOrNull()
            if (currentThemes.isNullOrEmpty()) {
                _isLoading.value = true
            }

            _error.value = null

            repository.syncThemes().onFailure { exception ->
                if (currentThemes.isNullOrEmpty()) {
                    _error.value = "Erro ao carregar temas: ${exception.localizedMessage}"
                } else {
                    Log.w("ThemesViewModel", "Falha no sync silencioso: ${exception.message}")
                }
            }
            _isLoading.value = false
        }
    }
}