package br.app.ide.ouvindoabiblia.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    // Une os dois fluxos do repositório (Bíblia e Estudos) num único estado LCE.
    val uiState: StateFlow<FavoritesUiState> = combine(
        repository.getFavorites(),
        repository.getFavoriteStudyLessons()
    ) { bible, studies ->
        FavoritesUiState.Success(bible, studies) as FavoritesUiState
    }.catch { throwable ->
        emit(FavoritesUiState.Error(throwable.message ?: "Erro ao carregar favoritos"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FavoritesUiState.Loading
    )

    fun handle(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.RemoveChapter -> viewModelScope.launch {
                repository.toggleFavorite(intent.chapterId, false)
            }

            is FavoritesIntent.RemoveStudy -> viewModelScope.launch {
                repository.toggleStudyFavorite(intent.studyId, intent.lessonId, false)
            }
        }
    }
}
