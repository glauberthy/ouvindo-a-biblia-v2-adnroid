package br.app.ide.ouvindoabiblia.ui.studies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Lesson
import br.app.ide.ouvindoabiblia.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StudyDetailsViewModel @Inject constructor(
    private val repository: BibleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Extrai os argumentos de navegação via Type-Safe Routing
    private val args = savedStateHandle.toRoute<Screen.StudyDetails>()
    val studyId: Int = args.studyId
    val studyTitle: String = args.studyTitle

    // ISSUE 6.B: refreshTrigger + flatMapLatest (idem ThemeDetails/Home). Evita o vazamento de
    // coletores: antes, cada Retry lançava um novo collect{} sobre um Flow de Room que nunca
    // completa, sem cancelar o anterior. WhileSubscribed(5s) padrão do app.
    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<StudyDetailsUiState> = refreshTrigger
        .flatMapLatest {
            repository.getStudyWithLessons(studyId)
                .map { study -> StudyDetailsUiState.Success(study = study) as StudyDetailsUiState }
                .onStart { emit(StudyDetailsUiState.Loading) }
                .catch { e ->
                    emit(
                        StudyDetailsUiState.Error(
                            message = e.message ?: "Erro desconhecido ao carregar o estudo."
                        )
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudyDetailsUiState.Loading
        )

    fun handle(intent: StudyDetailsIntent) {
        when (intent) {
            StudyDetailsIntent.Retry -> refreshTrigger.update { it + 1 }
        }
    }

    fun toggleFavorite(lesson: Lesson) {
        viewModelScope.launch {
            repository.toggleStudyFavorite(lesson.studyId, lesson.remoteId, !lesson.isFavorite)
        }
    }
}
