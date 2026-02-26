package br.app.ide.ouvindoabiblia.ui.themas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.app.ide.ouvindoabiblia.data.local.model.MomentWithAudio
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ThemeDetailsUiState {
    data object Loading : ThemeDetailsUiState
    data class Success(val moments: List<MomentWithAudio>) : ThemeDetailsUiState
    data class Error(val message: String) : ThemeDetailsUiState
}

@HiltViewModel
class ThemeDetailsViewModel @Inject constructor(
    private val repository: BibleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // SINALIZAÇÃO: Extração segura e tipada dos argumentos da rota!
    private val args = savedStateHandle.toRoute<Screen.ThemeDetails>()
    val themeTitle: String = args.themeTitle
    private val themeId: Int = args.themeId

    private val _uiState = MutableStateFlow<ThemeDetailsUiState>(ThemeDetailsUiState.Loading)
    val uiState: StateFlow<ThemeDetailsUiState> = _uiState.asStateFlow()

    init {
        loadMoments()
    }

    fun loadMoments() {
        viewModelScope.launch {
            _uiState.value = ThemeDetailsUiState.Loading
            repository.getMomentsForTheme(themeId)
                .catch { e ->
                    _uiState.value =
                        ThemeDetailsUiState.Error(e.message ?: "Erro ao carregar versículos")
                }
                .collect { moments ->
                    _uiState.value = ThemeDetailsUiState.Success(moments)
                }
        }
    }
}