package br.app.ide.ouvindoabiblia.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.data.repository.domain.Resource
import br.app.ide.ouvindoabiblia.data.repository.domain.model.MoreContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MoreViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    // ISSUE 3.B: sync + estados de loading orquestrados no repositório.
    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<MoreUiState> = refreshTrigger
        .flatMapLatest { repository.getMoreContentResource() }
        .map { it.toUiState() }
        .catch { throwable ->
            emit(MoreUiState.Error(throwable.message ?: "Erro ao carregar conteúdo"))
        }
        .stateIn(
            scope = viewModelScope,
            // WhileSubscribed(5s) (padrão idiomático, NÃO trocar por Lazy): compartilha o
            // stream; revisita <5s reusa sem novo GET; revisita >5s reinicia e re-checa
            // meta.version DE PROPÓSITO (version-gated, custo leve — só reescreve o Room se mudou).
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MoreUiState.Loading
        )

    fun handle(intent: MoreIntent) {
        when (intent) {
            is MoreIntent.Retry -> refreshTrigger.update { it + 1 }
        }
    }

    private fun Resource<MoreContent>.toUiState(): MoreUiState = when (this) {
        Resource.Loading -> MoreUiState.Loading
        is Resource.Error -> MoreUiState.Error(message)
        is Resource.Success -> MoreUiState.Success(data)
    }
}
