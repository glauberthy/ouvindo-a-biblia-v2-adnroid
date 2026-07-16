package br.app.ide.ouvindoabiblia.ui.studies

import br.app.ide.ouvindoabiblia.data.repository.domain.model.Study

sealed interface StudiesUiState {
    data object Loading : StudiesUiState
    data object Empty : StudiesUiState
    data class Error(val message: String) : StudiesUiState
    data class Success(
        val studies: List<Study>
    ) : StudiesUiState
}

sealed interface StudiesIntent {
    data object Retry : StudiesIntent
}