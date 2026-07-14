package br.app.ide.ouvindoabiblia.ui.themas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.data.repository.domain.Resource
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Theme
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
class ThemesViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    // ISSUE 3.B: sync version-gated 1x por load orquestrado no repositório.
    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<ThemesUiState> = refreshTrigger
        .flatMapLatest { repository.getThemesResource() }
        .map { it.toUiState() }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            // WhileSubscribed(5s) (padrão idiomático, NÃO trocar por Lazy): compartilha o
            // stream; revisita <5s reusa sem novo GET; revisita >5s reinicia e re-checa
            // meta.version DE PROPÓSITO (version-gated, custo leve — só reescreve o Room se mudou).
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemesUiState.Loading
        )

    fun handle(intent: ThemesIntent) {
        when (intent) {
            is ThemesIntent.Retry -> refreshTrigger.update { it + 1 }
            is ThemesIntent.SelectTheme -> { /* Navegação tratada na Screen */ }
        }
    }

    private fun Resource<List<Theme>>.toUiState(): ThemesUiState = when (this) {
        Resource.Loading -> ThemesUiState.Loading
        is Resource.Error -> ThemesUiState.Error(message)
        is Resource.Success ->
            if (data.isEmpty()) {
                ThemesUiState.Error("Nenhum tema encontrado. Verifique sua conexão e tente novamente.")
            } else {
                ThemesUiState.Success(themes = data)
            }
    }
}
