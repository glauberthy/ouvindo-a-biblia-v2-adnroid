package br.app.ide.ouvindoabiblia.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreContentDto
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MoreUiState {
    data object Loading : MoreUiState
    data class Success(val content: MoreContentDto) : MoreUiState
    data class Error(val message: String) : MoreUiState
}

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    val uiState: StateFlow<MoreUiState> =
        repository.getMoreContent()
            .map<MoreContentDto?, MoreUiState> { content ->
                if (content != null) {
                    MoreUiState.Success(content)
                } else {
                    MoreUiState.Loading
                }
            }
            .catch { throwable ->
                emit(
                    MoreUiState.Error(
                        throwable.message ?: "Erro ao carregar conteúdo"
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MoreUiState.Loading
            )

    init {
        sync()
    }

    fun sync() {
        viewModelScope.launch {
            repository.syncMoreContent()
        }
    }
}