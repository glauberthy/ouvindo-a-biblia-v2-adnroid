package br.app.ide.ouvindoabiblia.ui.studies

import br.app.ide.ouvindoabiblia.data.local.model.StudyWithLessons

sealed interface StudiesUiState {
    data object Loading : StudiesUiState
    data object Empty : StudiesUiState
    data class Error(val message: String) : StudiesUiState
    data class Success(
        val studies: List<StudyWithLessons>
    ) : StudiesUiState
}

sealed interface StudiesIntent {
    data object Retry : StudiesIntent
    data class SelectStudy(val studyId: Int) : StudiesIntent
}