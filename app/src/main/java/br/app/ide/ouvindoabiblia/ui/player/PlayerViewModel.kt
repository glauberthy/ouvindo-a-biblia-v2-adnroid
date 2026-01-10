package br.app.ide.ouvindoabiblia.ui.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BibleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // Variáveis para guardar o pedido se o serviço ainda não estiver pronto
    private var pendingBookId: String? = null
    private var pendingBookTitle: String? = null
    private var pendingCoverUrl: String? = null

    // Variável para evitar recarregar a playlist se já estiver tocando o mesmo livro
    private var currentBookId: String? = null

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()

                // --- CORREÇÃO: Verifica se tem algo pendente para tocar ---
                if (pendingBookId != null) {
//                    Log.d(
//                        "PlayerVM",
//                        "Serviço conectado! Carregando livro pendente: $pendingBookTitle"
//                    )
                    loadBookPlaylist(pendingBookId!!, pendingBookTitle!!, pendingCoverUrl!!)
                    // Limpa os pendentes
                    pendingBookId = null
                    pendingBookTitle = null
                    pendingCoverUrl = null
                } else {
                    updateStateFromPlayer() // Apenas atualiza UI se não tiver nada novo
                }

                startProgressLoop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun loadBookPlaylist(bookId: String, initialBookTitle: String, initialCoverUrl: String) {
        // Atualiza UI visualmente (Loading state ou dados iniciais)
        _uiState.update {
            it.copy(title = initialBookTitle, imageUrl = initialCoverUrl)
        }

        // --- CORREÇÃO: Se o controller for nulo, guarda para depois ---
        if (mediaController == null) {
            Log.d("PlayerVM", "Controller ainda nulo. Guardando pedido para depois.")
            pendingBookId = bookId
            pendingBookTitle = initialBookTitle
            pendingCoverUrl = initialCoverUrl
            return
        }

        // Se já estamos tocando este livro, não recarrega (apenas abre a tela)
        // Dica: Adicionei !isPlaying para forçar o play se estiver pausado
        if (currentBookId == bookId && (mediaController?.mediaItemCount ?: 0) > 0) {
            if (mediaController?.isPlaying == false) {
                mediaController?.play()
            }
            return
        }

        currentBookId = bookId

        viewModelScope.launch {
            Log.d("PlayerVM", "Buscando capítulos de $bookId no banco...")
            val chapters = repository.getChapters(bookId).first()

            if (chapters.isNotEmpty()) {
                Log.d("PlayerVM", "Encontrados ${chapters.size} capítulos. Configurando playlist.")
                setupPlaylist(chapters)
            } else {
                Log.e("PlayerVM", "Nenhum capítulo encontrado para $bookId")
            }
        }
    }

    private fun setupPlaylist(chapters: List<br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo>) {
        val controller = mediaController ?: return

        val mediaItems = chapters.map { chapterInfo ->
            val metadata = MediaMetadata.Builder()
                .setTitle(chapterInfo.bookName)
                .setSubtitle("Capítulo ${chapterInfo.chapter.number}")
                .setArtworkUri(chapterInfo.coverUrl?.toUri())
                // Adicionamos metadados extras para facilitar
                .setAlbumTitle(chapterInfo.bookName)
                .build()

            MediaItem.Builder()
                .setUri(chapterInfo.chapter.audioUrl)
                .setMediaId(chapterInfo.chapter.id.toString())
                .setMediaMetadata(metadata)
                .build()
        }

        // Define a playlist, prepara e toca
        controller.setMediaItems(mediaItems, 0, 0L)
        controller.prepare()
        controller.play()

        Log.d("PlayerVM", "Playlist enviada ao ExoPlayer. Play!")
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateStateFromPlayer()
            }

            override fun onEvents(player: Player, events: Player.Events) {
                updateStateFromPlayer()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateStateFromPlayer()
            }
        })
    }

    private fun updateStateFromPlayer() {
        mediaController?.let { player ->
            val mediaItem = player.currentMediaItem

            // Extraindo o número do capítulo do subtítulo "Capítulo X"
            // Se falhar, tenta pegar do índice + 1
            val subtitle = mediaItem?.mediaMetadata?.subtitle?.toString() ?: ""

            _uiState.update { state ->
                state.copy(
                    isPlaying = player.isPlaying,
                    isBuffering = player.playbackState == Player.STATE_BUFFERING,
                    duration = if (player.duration > 0) player.duration else 0L,
                    title = mediaItem?.mediaMetadata?.title?.toString() ?: state.title,
                    subtitle = subtitle,
                    imageUrl = mediaItem?.mediaMetadata?.artworkUri?.toString() ?: state.imageUrl
                )
            }
        }
    }

    private fun startProgressLoop() {
        viewModelScope.launch {
            while (isActive) {
                mediaController?.let { player ->
                    if (player.isPlaying) {
                        _uiState.update { it.copy(currentPosition = player.currentPosition) }
                    }
                }
                delay(1000)
            }
        }
    }

    fun togglePlayPause() {
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _uiState.update { it.copy(currentPosition = positionMs) }
    }

    fun skipForward() {
        mediaController?.let { it.seekTo(it.currentPosition + 10_000) }
    }

    fun skipBack() {
        mediaController?.let { it.seekTo(it.currentPosition - 10_000) }
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}