package br.app.ide.ouvindoabiblia.ui.themas

import br.app.ide.ouvindoabiblia.data.local.entity.ThemeEntity
import br.app.ide.ouvindoabiblia.data.local.model.MomentWithAudio

// Estado da UI seguindo o padrão LCE (Loading, Content, Error).
sealed interface ThemeDetailsUiState {
    data object Loading : ThemeDetailsUiState
    data class Success(
        val theme: ThemeEntity,
        val moments: List<MomentWithAudio>
    ) : ThemeDetailsUiState

    data class Error(val message: String) : ThemeDetailsUiState
}

sealed interface ThemeDetailsIntent {
    data object Retry : ThemeDetailsIntent
}
