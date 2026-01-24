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
import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
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

    // Variável para controlar o "Job" (o trabalho de contagem regressiva)
    private var sleepTimerJob: Job? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // Variáveis pendentes (Mantive sua lógica original)
    private var pendingBookId: String? = null
    private var pendingBookTitle: String? = null
    private var pendingCoverUrl: String? = null

    init {
        connectToService()
    }


    // Função para configurar o Timer
    fun setSleepTimer(minutes: Int) {
        // 1. Cancela qualquer timer anterior (se o usuário mudar de ideia)
        sleepTimerJob?.cancel()

        // Se escolheu "0" ou "Desativar", paramos por aqui
        if (minutes <= 0) {
            return
        }

        // 2. Inicia a contagem regressiva
        sleepTimerJob = viewModelScope.launch {
            // Converte minutos para milissegundos
            val delayMs = minutes * 60 * 1000L

            // Espera o tempo passar...
            delay(delayMs)

            // 3. O tempo acabou! Pausa o player.
            pause() // Ou togglePlayPause() se você tiver lógica de verificação

            // Opcional: Zerar o job
            sleepTimerJob = null
        }
    }

    // Função auxiliar para garantir que pausa mesmo (se não tiver uma fun pause explicita)
    private fun pause() {
        if (_uiState.value.isPlaying) {
            mediaController?.pause()
            _uiState.update { it.copy(isPlaying = false) }
        }
    }

    private fun connectToService() {
        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()

                // === LÓGICA DE RESUME (SUA ORIGINAL) ===
                if (pendingBookId == "RESUME") {
                    val playingId =
                        mediaController?.currentMediaItem?.mediaMetadata?.extras?.getString("book_id")
                    if (playingId != null) {
                        loadBookPlaylist(playingId, "", "")
                    }
                    pendingBookId = null
                } else if (pendingBookId != null) {
                    loadBookPlaylist(pendingBookId!!, pendingBookTitle!!, pendingCoverUrl!!)
                    pendingBookId = null
                    pendingBookTitle = null
                    pendingCoverUrl = null
                } else {
                    updateStateFromPlayer()
                    loadCurrentBookChaptersList()
                }

                startProgressLoop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun loadBookPlaylist(bookId: String, initialBookTitle: String, initialCoverUrl: String) {
        if (bookId != "RESUME") {
            _uiState.update { it.copy(title = initialBookTitle, imageUrl = initialCoverUrl) }
        }

        if (mediaController == null) {
            pendingBookId = bookId
            pendingBookTitle = initialBookTitle
            pendingCoverUrl = initialCoverUrl
            return
        }

        val playingBookId =
            mediaController?.currentMediaItem?.mediaMetadata?.extras?.getString("book_id")

        if (playingBookId == bookId) {
            viewModelScope.launch {
                val chapters = repository.getChapters(bookId).first()
                _uiState.update { it.copy(chapters = chapters) }
                updateStateFromPlayer()
            }
            return
        }

        viewModelScope.launch {
            val chapters = repository.getChapters(bookId).first()
            if (chapters.isNotEmpty()) {
                setupPlaylist(chapters, bookId)
            }
        }
    }

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

    private fun setupPlaylist(chapters: List<ChapterWithBookInfo>, bookId: String) {
        val controller = mediaController ?: return
        _uiState.update { it.copy(chapters = chapters) }

        val mediaItems = chapters.map { chapterInfo ->
            val extras = Bundle()
            extras.putString("book_id", bookId)

            val metadata = MediaMetadata.Builder()
                .setTitle(chapterInfo.bookName)
                .setSubtitle("Capítulo ${chapterInfo.chapter.number}")
                .setArtworkUri(Uri.parse(chapterInfo.coverUrl))
                .setExtras(extras)
                .build()

            MediaItem.Builder()
                .setUri(chapterInfo.chapter.audioUrl)
                .setMediaId(chapterInfo.chapter.id.toString())
                .setMediaMetadata(metadata)
                .build()
        }

        controller.setMediaItems(mediaItems, 0, 0L)
        controller.prepare()
        controller.play()
    }

    // --- ATUALIZADO: Agora lê Shuffle e Repeat ---
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
                    currentChapterIndex = player.currentMediaItemIndex,
                    playbackSpeed = player.playbackParameters.speed,
                    isShuffleEnabled = player.shuffleModeEnabled,
                    repeatMode = player.repeatMode
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

            // Listener importante para quando o usuário clica em shuffle/repeat
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                updateStateFromPlayer()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
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

    // --- CONTROLES DE UI ---

    fun togglePlayPause() {
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    // Usado pela Barra de Progresso
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        // Atualiza a UI imediatamente para o slider não "pular"
        _uiState.update { it.copy(currentPosition = positionMs) }
    }

    // Usado pelo botão da Direita (+10)
    fun fastForward() {
        mediaController?.let {
            it.seekTo(it.currentPosition + 10_000)
        }
    }

    // Usado pelo botão da Esquerda (-10)
    fun rewind() {
        mediaController?.let {
            it.seekTo(it.currentPosition - 10_000)
        }
    }

    // NOVO: Pular para próxima FAIXA (Capítulo)
    fun skipToNextChapter() {
        if (mediaController?.hasNextMediaItem() == true) {
            mediaController?.seekToNextMediaItem()
        }
    }

    // NOVO: Voltar para FAIXA anterior
    fun skipToPreviousChapter() {
        if (mediaController?.hasPreviousMediaItem() == true) {
            mediaController?.seekToPreviousMediaItem()
        }
    }

    // NOVO: Aleatório
    fun toggleShuffle() {
        mediaController?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    // NOVO: Repetir (Ciclo: OFF -> ONE -> ALL -> OFF)
    fun toggleRepeat() {
        mediaController?.let {
            val newMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            it.repeatMode = newMode
        }
    }

    fun onChapterSelected(index: Int) {
        mediaController?.seekTo(index, 0L)
        if (mediaController?.isPlaying == false) mediaController?.play()
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}