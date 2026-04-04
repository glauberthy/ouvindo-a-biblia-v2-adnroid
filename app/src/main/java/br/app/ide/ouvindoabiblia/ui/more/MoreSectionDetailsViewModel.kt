package br.app.ide.ouvindoabiblia.ui.more

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreSectionDto
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MoreSectionDetailsUiState {
    data object Loading : MoreSectionDetailsUiState
    data class Success(val section: MoreSectionDto) : MoreSectionDetailsUiState
    data class Error(val message: String) : MoreSectionDetailsUiState
}

@HiltViewModel
class MoreSectionDetailsViewModel @Inject constructor(
    repository: BibleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sectionId: String = checkNotNull(savedStateHandle["sectionId"])

    val uiState: StateFlow<MoreSectionDetailsUiState> =
        repository.getMoreContent()
            .map { moreContent ->
                when {
                    moreContent == null -> MoreSectionDetailsUiState.Loading
                    else -> {
                        val section = moreContent.sections.firstOrNull { it.id == sectionId }
                        if (section != null) {
                            MoreSectionDetailsUiState.Success(section)
                        } else {
                            MoreSectionDetailsUiState.Error("Seção não encontrada.")
                        }
                    }
                }
            }
            .catch { throwable ->
                emit(
                    MoreSectionDetailsUiState.Error(
                        throwable.message ?: "Erro ao carregar seção."
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MoreSectionDetailsUiState.Loading
            )
}