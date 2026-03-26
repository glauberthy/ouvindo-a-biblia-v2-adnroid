package br.app.ide.ouvindoabiblia.ui.studies

import br.app.ide.ouvindoabiblia.data.local.model.StudyWithLessons

sealed interface StudiesUiState {
    data object Loading : StudiesUiState
    data class Error(val message: String) : StudiesUiState
    data class Success(
        val studies: List<StudyWithLessons>
    ) : StudiesUiState
}

sealed interface StudiesIntent {
    data object Retry : StudiesIntent

    // A navegação real (SelectStudy) será tratada no clique do componente na UI,
    // mas deixamos mapeado caso precise de logica de analitics futura.
    data class SelectStudy(val studyId: Int) : StudiesIntent
}