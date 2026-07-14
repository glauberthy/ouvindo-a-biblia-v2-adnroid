package br.app.ide.ouvindoabiblia.ui.studies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.data.repository.domain.Resource
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Study
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StudiesViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    // ISSUE 3.B: orquestração de loading vive no repositório (getStudiesResource).
    // refreshTrigger + flatMapLatest = re-dispara o stream (novo sync) no Retry.
    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<StudiesUiState> = refreshTrigger
        .flatMapLatest { repository.getStudiesResource() }
        .map { it.toUiState() }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudiesUiState.Loading
        )

    fun handle(intent: StudiesIntent) {
        when (intent) {
            is StudiesIntent.Retry -> refreshTrigger.update { it + 1 }
            is StudiesIntent.SelectStudy -> { /* Navegação pela UI */ }
        }
    }

    private fun Resource<List<Study>>.toUiState(): StudiesUiState = when (this) {
        Resource.Loading -> StudiesUiState.Loading
        is Resource.Error -> StudiesUiState.Error(message)
        is Resource.Success ->
            if (data.isEmpty()) StudiesUiState.Empty else StudiesUiState.Success(studies = data)
    }
}
