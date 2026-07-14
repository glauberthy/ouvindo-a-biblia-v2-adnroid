package br.app.ide.ouvindoabiblia.ui.favorites

import br.app.ide.ouvindoabiblia.data.repository.domain.model.Chapter
import br.app.ide.ouvindoabiblia.data.repository.domain.model.FavoriteLesson

// Estado da UI seguindo o padrão LCE (Loading, Content, Error).
sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState
    data class Success(
        val bibleFavorites: List<Chapter> = emptyList(),
        val studyFavorites: List<FavoriteLesson> = emptyList()
    ) : FavoritesUiState

    data class Error(val message: String) : FavoritesUiState
}

sealed interface FavoritesIntent {
    data class RemoveChapter(val chapterId: Long) : FavoritesIntent
    data class RemoveStudy(val studyId: Int, val lessonId: Int) : FavoritesIntent
}
