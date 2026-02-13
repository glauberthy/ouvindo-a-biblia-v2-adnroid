package br.app.ide.ouvindoabiblia.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FavoritesUiState {
    object Loading : FavoritesUiState
    data class Success(val favorites: List<ChapterWithBookInfo>) : FavoritesUiState
    object Empty : FavoritesUiState
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    // Observa o Flow do banco de dados em tempo real
    val uiState: StateFlow<FavoritesUiState> = repository.getFavorites()
        .map { list ->
            if (list.isEmpty()) FavoritesUiState.Empty
            else FavoritesUiState.Success(list)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FavoritesUiState.Loading
        )

    fun removeFromFavorites(chapterId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(chapterId, false)
        }
    }
}