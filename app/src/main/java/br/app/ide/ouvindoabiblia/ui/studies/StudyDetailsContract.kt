package br.app.ide.ouvindoabiblia.ui.studies

import br.app.ide.ouvindoabiblia.data.local.model.StudyWithLessons

sealed interface StudyDetailsUiState {
    data object Loading : StudyDetailsUiState
    data class Error(val message: String) : StudyDetailsUiState
    data class Success(
        val studyWithLessons: StudyWithLessons
    ) : StudyDetailsUiState
}

sealed interface StudyDetailsIntent {
    data object Retry : StudyDetailsIntent
}