package br.app.ide.ouvindoabiblia.ui.themas

import br.app.ide.ouvindoabiblia.data.repository.domain.model.Theme

sealed interface ThemesUiState {
    object Loading : ThemesUiState
    data class Error(val message: String) : ThemesUiState
    data class Success(
        val themes: List<Theme>
    ) : ThemesUiState
}

sealed interface ThemesIntent {
    object Retry : ThemesIntent
    data class SelectTheme(val themeId: Int) : ThemesIntent
}