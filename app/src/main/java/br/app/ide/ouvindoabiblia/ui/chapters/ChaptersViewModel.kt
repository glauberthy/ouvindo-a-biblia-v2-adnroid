package br.app.ide.ouvindoabiblia.ui.chapters

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.toRoute
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.service.PlaybackService
import br.app.ide.ouvindoabiblia.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ChaptersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: BibleRepository,
    @ApplicationContext private val context: Context // Injeta Contexto para criar o Controller
) : ViewModel() {

    private val args = savedStateHandle.toRoute<Screen.Chapters>()
    val bookName: String = args.bookName

    // 2. Mapeamento atualizado com a URL
    val chapters: StateFlow<List<ChapterUiModel>> = repository.getChapters(args.bookId)
        .map { entities ->
            entities.map { entity ->
                ChapterUiModel(
                    number = entity.number, // ou entity.capitulo, verifique sua entidade
                    id = entity.id,
                    url = entity.audioUrl
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // --- LÓGICA DO PLAYER (Media3) ---

    private var mediaController: MediaController? = null
    private val controllerFuture = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
    ).buildAsync()

    init {
        // Conecta ao Serviço assim que a tela abre
        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun playChapter(chapter: ChapterUiModel) {
        mediaController?.let { controller ->
            // Cria o item de mídia com a URL do capítulo
            val mediaItem = MediaItem.fromUri(chapter.url)

            // Configura e toca
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()

            // Opcional: Log para confirmar
            android.util.Log.d("Player", "Tocando: ${chapter.url}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Libera a conexão com o serviço para não vazar memória
        MediaController.releaseFuture(controllerFuture)
    }
}

data class ChapterUiModel(
    val number: Int,
    val id: Long,
    val url: String
)