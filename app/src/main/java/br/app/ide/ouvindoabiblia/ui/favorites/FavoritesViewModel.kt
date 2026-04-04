package br.app.ide.ouvindoabiblia.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo
import br.app.ide.ouvindoabiblia.data.local.model.FavoriteStudyLessonDto
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. Estado da UI agora carrega as duas listas simultaneamente
data class FavoritesUiState(
    val bibleFavorites: List<ChapterWithBookInfo> = emptyList(),
    val studyFavorites: List<FavoriteStudyLessonDto> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: BibleRepository
) : ViewModel() {

    // 2. Unindo os dois fluxos do Repositório (Bíblia e Estudos)
    // No FavoritesViewModel.kt
    val uiState: StateFlow<FavoritesUiState> = combine(
        repository.getFavorites(),
        repository.getFavoriteStudyLessons()
    ) { bible, studies ->
        // LOG AQUI
        android.util.Log.d("DEBUG_FAV", "Bíblia: ${bible.size} | Estudos: ${studies.size}")

        FavoritesUiState(bible, studies, false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FavoritesUiState()
    )

    // 3. Ação para remover favorito da Bíblia
    fun removeFromFavorites(chapterId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(chapterId, false)
        }
    }

    // 4. Nova ação para remover favorito de Estudos
    fun removeStudyFromFavorites(studyId: Int, lessonId: Int) {
        viewModelScope.launch {
            repository.toggleStudyFavorite(studyId, lessonId, false)
        }
    }
}