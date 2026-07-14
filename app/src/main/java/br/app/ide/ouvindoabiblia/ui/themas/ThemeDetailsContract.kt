package br.app.ide.ouvindoabiblia.ui.themas

import br.app.ide.ouvindoabiblia.data.repository.domain.model.Moment
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Theme

// Estado da UI seguindo o padrão LCE (Loading, Content, Error).
sealed interface ThemeDetailsUiState {
    data object Loading : ThemeDetailsUiState
    data class Success(
        val theme: Theme,
        val moments: List<Moment>
    ) : ThemeDetailsUiState

    data class Error(val message: String) : ThemeDetailsUiState
}

sealed interface ThemeDetailsIntent {
    data object Retry : ThemeDetailsIntent
}
