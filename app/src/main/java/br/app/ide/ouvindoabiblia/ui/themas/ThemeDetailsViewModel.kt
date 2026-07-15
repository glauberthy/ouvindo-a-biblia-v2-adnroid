package br.app.ide.ouvindoabiblia.ui.themas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ThemeDetailsViewModel @Inject constructor(
    private val repository: BibleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // SINALIZAÇÃO: Extração segura e tipada dos argumentos da rota!
    private val args = savedStateHandle.toRoute<Screen.ThemeDetails>()
    val themeTitle: String = args.themeTitle
    private val themeId: Int = args.themeId

    // ISSUE 6.B: refreshTrigger + flatMapLatest (padrão idiomático das demais VMs). O
    // flatMapLatest cancela o coletor anterior a cada Retry; antes, cada Retry lançava um novo
    // collect{} sobre Flows de Room que nunca completam, sem cancelar o anterior → N coletores
    // permanentes escrevendo no mesmo estado. WhileSubscribed(5s) igual a Home/Themes/Studies/More.
    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<ThemeDetailsUiState> = refreshTrigger
        .flatMapLatest {
            combine(
                repository.getThemeById(themeId),
                repository.getMomentsForTheme(themeId)
            ) { theme, moments ->
                if (theme == null) {
                    ThemeDetailsUiState.Error("Tema não encontrado")
                } else {
                    ThemeDetailsUiState.Success(theme = theme, moments = moments)
                }
            }
                .onStart { emit(ThemeDetailsUiState.Loading) }
                .catch { e ->
                    emit(ThemeDetailsUiState.Error(e.message ?: "Erro ao carregar tema"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeDetailsUiState.Loading
        )

    fun handle(intent: ThemeDetailsIntent) {
        when (intent) {
            is ThemeDetailsIntent.Retry -> refreshTrigger.update { it + 1 }
        }
    }
}
