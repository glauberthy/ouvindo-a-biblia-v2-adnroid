package br.app.ide.ouvindoabiblia.ui.studies

import br.app.ide.ouvindoabiblia.data.repository.domain.model.Study

sealed interface StudyDetailsUiState {
    data object Loading : StudyDetailsUiState
    data class Error(val message: String) : StudyDetailsUiState
    data class Success(
        val study: Study
    ) : StudyDetailsUiState
}

sealed interface StudyDetailsIntent {
    data object Retry : StudyDetailsIntent
}