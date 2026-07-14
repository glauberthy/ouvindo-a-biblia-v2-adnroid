package br.app.ide.ouvindoabiblia.ui.more

import br.app.ide.ouvindoabiblia.data.remote.dto.MoreContentDto

// Estado da UI seguindo o padrão LCE (Loading, Content, Error).
sealed interface MoreUiState {
    data object Loading : MoreUiState
    data class Success(val content: MoreContentDto) : MoreUiState
    data class Error(val message: String) : MoreUiState
}

sealed interface MoreIntent {
    data object Retry : MoreIntent
}
