package br.app.ide.ouvindoabiblia.ui.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
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

    // Variáveis para guardar o pedido se o serviço ainda não estiver conectado
    private var pendingBookId: String? = null
    private var pendingBookTitle: String? = null
    private var pendingCoverUrl: String? = null

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

                // Conectou! Verifica se tinha algo pendente para tocar
                if (pendingBookId != null) {
                    loadBookPlaylist(pendingBookId!!, pendingBookTitle!!, pendingCoverUrl!!)
                    // Limpa pendências
                    pendingBookId = null
                    pendingBookTitle = null
                    pendingCoverUrl = null
                } else {
                    // Se não tinha nada pendente, apenas atualiza a tela com o que já está tocando
                    updateStateFromPlayer()
                    // Tenta carregar a lista de capítulos do livro atual para a gaveta funcionar
                    loadCurrentBookChaptersList()
                }

                startProgressLoop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Chamado pela MainActivity ao entrar na tela
    fun loadBookPlaylist(bookId: String, initialBookTitle: String, initialCoverUrl: String) {
        // 1. Atualiza a UI visualmente de imediato (para não ficar tudo branco)
        _uiState.update {
            it.copy(title = initialBookTitle, imageUrl = initialCoverUrl)
        }

        // 2. Se o serviço ainda não conectou, guarda para depois
        if (mediaController == null) {
            pendingBookId = bookId
            pendingBookTitle = initialBookTitle
            pendingCoverUrl = initialCoverUrl
            return
        }

        // 3. A LÓGICA DO CRACHÁ (Evita reiniciar se for o mesmo livro)
        // Lemos os extras do áudio atual para ver o ID
        val playingBookId =
            mediaController?.currentMediaItem?.mediaMetadata?.extras?.getString("book_id")

        if (playingBookId == bookId) {
//            Log.d("PlayerVM", "O livro $bookId já está tocando. Mantendo reprodução.")

            // Mesmo sem reiniciar o áudio, precisamos buscar a lista de capítulos
            // para que a gaveta (Bottom Sheet) funcione corretamente.
            viewModelScope.launch {
                val chapters = repository.getChapters(bookId).first()
                _uiState.update { it.copy(chapters = chapters) }
                updateStateFromPlayer() // Sincroniza qual capítulo está em negrito
            }
            return
        }

        // 4. Se for um livro diferente, carrega a playlist do zero
//        Log.d("PlayerVM", "Trocando de livro: $playingBookId -> $bookId")
        viewModelScope.launch {
            val chapters = repository.getChapters(bookId).first()
            if (chapters.isNotEmpty()) {
                setupPlaylist(chapters, bookId)
            }
        }
    }

    // Função auxiliar: Recupera a lista de capítulos se o usuário reabrir o app já tocando
    private fun loadCurrentBookChaptersList() {
        val playingBookId =
            mediaController?.currentMediaItem?.mediaMetadata?.extras?.getString("book_id")
        if (playingBookId != null) {
            viewModelScope.launch {
                val chapters = repository.getChapters(playingBookId).first()
                _uiState.update { it.copy(chapters = chapters) }
            }
        }
    }

    private fun setupPlaylist(
        chapters: List<br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo>,
        bookId: String
    ) {
        val controller = mediaController ?: return

        // Atualiza a lista na UI (Gaveta)
        _uiState.update { it.copy(chapters = chapters) }

        val mediaItems = chapters.map { chapterInfo ->
            // --- AQUI FAZEMOS A "TATUAGEM" DO ID ---
            val extras = Bundle()
            extras.putString("book_id", bookId)

            val metadata = MediaMetadata.Builder()
                .setTitle(chapterInfo.bookName)
                .setSubtitle("Capítulo ${chapterInfo.chapter.number}")
                .setArtworkUri(Uri.parse(chapterInfo.coverUrl))
                .setExtras(extras) // <--- Anexa o ID no áudio
                .build()

            MediaItem.Builder()
                .setUri(chapterInfo.chapter.audioUrl)
                .setMediaId(chapterInfo.chapter.id.toString())
                .setMediaMetadata(metadata)
                .build()
        }

        // Define a playlist e começa do índice 0
        controller.setMediaItems(mediaItems, 0, 0L)
        controller.prepare()
        controller.play()
    }

    // Sincroniza o estado do Player com a UI (Textos, Play/Pause, Barra)
    private fun updateStateFromPlayer() {
        mediaController?.let { player ->
            val mediaItem = player.currentMediaItem
            val subtitle = mediaItem?.mediaMetadata?.subtitle?.toString() ?: ""

            _uiState.update { state ->
                state.copy(
                    isPlaying = player.isPlaying,
                    isBuffering = player.playbackState == Player.STATE_BUFFERING,
                    duration = if (player.duration > 0) player.duration else 0L,
                    title = mediaItem?.mediaMetadata?.title?.toString() ?: state.title,
                    subtitle = subtitle,
                    imageUrl = mediaItem?.mediaMetadata?.artworkUri?.toString() ?: state.imageUrl,
                    currentChapterIndex = player.currentMediaItemIndex
                )
            }
        }
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

    // --- Ações da UI ---
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

    // Usado pela Gaveta de Capítulos
    fun onChapterSelected(index: Int) {
        mediaController?.seekTo(index, 0L)
        if (mediaController?.isPlaying == false) mediaController?.play()
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}