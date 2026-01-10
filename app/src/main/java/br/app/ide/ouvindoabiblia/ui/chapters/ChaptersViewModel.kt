package br.app.ide.ouvindoabiblia.ui.chapters

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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

    val chapters: StateFlow<List<ChapterUiModel>> = repository.getChapters(args.bookId)
        .map { list ->
            list.map { item ->
                ChapterUiModel(
                    // Dados do Capítulo (estão dentro do objeto 'chapter')
                    number = item.chapter.number,
                    id = item.chapter.id,
                    url = item.chapter.audioUrl,
                    bookName = item.bookName,
                    coverUrl = item.coverUrl
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
            val metadata = MediaMetadata.Builder()
                .setTitle("Capítulo ${chapter.number}")
                .setArtist(chapter.bookName)
                .setArtworkUri((chapter.coverUrl ?: "").toUri())
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(chapter.url)
                .setMediaMetadata(metadata)
                .build()

            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Libera a conexão com o serviço para não vazar memória
        MediaController.releaseFuture(controllerFuture)
    }
}

// Modelo de UI (Data Class)
data class ChapterUiModel(
    val number: Int,
    val id: Long,
    val url: String,
    val bookName: String,
    val coverUrl: String?
)