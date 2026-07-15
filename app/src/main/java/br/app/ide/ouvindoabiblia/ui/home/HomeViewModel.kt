package br.app.ide.ouvindoabiblia.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.data.repository.domain.Resource
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Book
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    // ISSUE 3.B: loading/sync orquestrados no repositório (getBooksResource).
    // O filtro é combinado por cima do recurso — trocar filtro NÃO re-dispara sync.
    private val refreshTrigger = MutableStateFlow(0)
    private val _selectedFilter = MutableStateFlow(TestamentFilter.ALL)

    val uiState: StateFlow<HomeUiState> = combine(
        refreshTrigger.flatMapLatest { repository.getBooksResource() },
        _selectedFilter
    ) { resource, filter ->
        resource.toUiState(filter)
    }
        .flowOn(Dispatchers.Default) // Processamento (sort/map/filter) fora da Main Thread
        .stateIn(
            scope = viewModelScope,
            // WhileSubscribed(5s) (padrão idiomático, NÃO trocar por Lazy): compartilha o
            // stream; revisita <5s reusa sem novo GET; revisita >5s reinicia e re-checa
            // meta.version DE PROPÓSITO (version-gated, custo leve — só reescreve o Room se mudou).
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading
        )

    fun handle(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SelectFilter -> _selectedFilter.value = intent.filter
            is HomeIntent.Retry -> refreshTrigger.update { it + 1 }
            is HomeIntent.OpenBook -> { /* Navegação tratada na UI */ }
        }
    }

    private fun Resource<List<Book>>.toUiState(filter: TestamentFilter): HomeUiState =
        when (this) {
            Resource.Loading -> HomeUiState.Loading
            is Resource.Error -> HomeUiState.Error(message)
            is Resource.Success -> {
                if (data.isEmpty()) {
                    // ISSUE 6.D: sync OK porém 0 livros é estado TERMINAL, não transitório — o
                    // syncedListResource só emite Success depois de o sync concluir, então não há
                    // "empty passageiro". Antes isto virava Loading eterno sem saída; agora é Error
                    // com Retry (coerente com Themes: empty→Error).
                    HomeUiState.Error("Nenhum livro disponível. Tente novamente.")
                } else {
                    val bookSummaries = data
                        .sortedBy { it.numericId }
                        .map {
                            BookSummary(
                                id = it.numericId,
                                title = it.name,
                                imageUrl = if (it.imageUrl.isNullOrBlank()) null else it.imageUrl,
                                testament = it.testament
                            )
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
            }
        }
}
