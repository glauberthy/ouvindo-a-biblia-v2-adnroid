package br.app.ide.ouvindoabiblia.ui.themas

import br.app.ide.ouvindoabiblia.data.local.entity.ThemeEntity

sealed interface ThemesUiState {
    object Loading : ThemesUiState
    data class Error(val message: String) : ThemesUiState
    data class Success(
        val themes: List<ThemeEntity>
    ) : ThemesUiState
}

sealed interface ThemesIntent {
    object Retry : ThemesIntent
    data class SelectTheme(val themeId: Int) : ThemesIntent
}