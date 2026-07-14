package br.app.ide.ouvindoabiblia.ui.studies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Lesson
import br.app.ide.ouvindoabiblia.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudyDetailsViewModel @Inject constructor(
    private val repository: BibleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Extrai os argumentos de navegação via Type-Safe Routing
    private val args = savedStateHandle.toRoute<Screen.StudyDetails>()
    val studyId: Int = args.studyId
    val studyTitle: String = args.studyTitle

    private val _uiState = MutableStateFlow<StudyDetailsUiState>(StudyDetailsUiState.Loading)
    val uiState: StateFlow<StudyDetailsUiState> = _uiState.asStateFlow()

    init {
        loadStudyDetails()
    }

    fun handle(intent: StudyDetailsIntent) {
        when (intent) {
            StudyDetailsIntent.Retry -> loadStudyDetails()
        }
    }

    fun loadStudyDetails() {
        viewModelScope.launch {
            _uiState.value = StudyDetailsUiState.Loading

            repository.getStudyWithLessons(studyId)
                .catch { e ->
                    _uiState.value = StudyDetailsUiState.Error(
                        message = e.message ?: "Erro desconhecido ao carregar o estudo."
                    )
                }
                .collect { studyData ->
                    // studyData é o Study de domínio (dados do estudo + List<Lesson>)
                    _uiState.value = StudyDetailsUiState.Success(
                        study = studyData
                    )
                }
        }
    }

    fun toggleFavorite(lesson: Lesson) {
        viewModelScope.launch {
            repository.toggleStudyFavorite(lesson.studyId, lesson.remoteId, !lesson.isFavorite)
        }
    }
}