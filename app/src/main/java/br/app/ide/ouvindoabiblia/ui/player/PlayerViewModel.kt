package br.app.ide.ouvindoabiblia.ui.player

// Imports do Google Cast
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
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
import com.google.android.gms.cast.MediaMetadata as CastMediaMetadata

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BibleRepository
) : ViewModel() {

    // --- CAST VARS ---
    private var castContext: CastContext? = null
    private var castSession: CastSession? = null

    // Ouvinte da TV
    private val castCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            updateStateFromCast()
            checkCastCompletion()
        }

        override fun onMetadataUpdated() {
            updateStateFromCast()
        }
    }

    // Gerenciador de Sessão
    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {}

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            castSession = session
            session.remoteMediaClient?.registerCallback(castCallback)

            // Conectou? Manda o capítulo ATUAL da UI imediatamente
            val currentChapter =
                _uiState.value.chapters.getOrNull(_uiState.value.currentChapterIndex)
            if (currentChapter != null) {
                loadMediaOnCast(currentChapter, _uiState.value.currentPosition)
            }
            mediaController?.pause()
            updateStateFromCast()
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionEnding(session: CastSession) {
            session.remoteMediaClient?.unregisterCallback(castCallback)
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            castSession = null
            _uiState.update { it.copy(isPlaying = false) }
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            castSession = session
            session.remoteMediaClient?.registerCallback(castCallback)
            updateStateFromCast()
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    private var sleepTimerJob: Job? = null
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingBookId: String? = null
    private var pendingBookTitle: String? = null
    private var pendingCoverUrl: String? = null

    init {
        try {
            castContext = CastContext.getSharedInstance(context)
            castContext?.sessionManager?.addSessionManagerListener(
                sessionManagerListener,
                CastSession::class.java
            )
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Erro Cast: ${e.message}")
        }
        connectToService()
    }

    private fun updateStateFromCast() {
        val session = castSession ?: return
        if (!session.isConnected) return
        val remote = session.remoteMediaClient ?: return

        // Evita update se o RemoteClient estiver nulo ou instável
        val isPlaying = remote.isPlaying
        val duration = remote.streamDuration
        val finalDuration = if (duration > 0) duration else _uiState.value.duration

        _uiState.update {
            it.copy(
                isPlaying = isPlaying,
                duration = finalDuration,
                isBuffering = remote.isBuffering
            )
        }
    }

    private fun checkCastCompletion() {
        val remote = castSession?.remoteMediaClient ?: return
        if (remote.playerState == MediaStatus.PLAYER_STATE_IDLE &&
            remote.idleReason == MediaStatus.IDLE_REASON_FINISHED
        ) {
            skipToNextChapter()
        }
    }

    // --- NOVA FUNÇÃO ROBUSTA: Recebe o capítulo exato ---
    private fun loadMediaOnCast(
        chapter: ChapterWithBookInfo,
        positionMs: Long = 0,
        autoPlay: Boolean = true
    ) {
        val session = castSession ?: return
        val remoteMediaClient = session.remoteMediaClient ?: return

        // Usa os dados do capítulo passado, e não do State (que pode estar desatualizado)
        val currentState = _uiState.value

        val movieMetadata = CastMediaMetadata(CastMediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
        movieMetadata.putString(CastMediaMetadata.KEY_TITLE, currentState.title) // Livro
        movieMetadata.putString(
            CastMediaMetadata.KEY_SUBTITLE,
            "Capítulo ${chapter.chapter.number}"
        )

        // Usa a capa do capítulo ou a capa geral do livro
        val coverToUse = chapter.coverUrl ?: currentState.imageUrl
        if (coverToUse.isNotEmpty()) {
            movieMetadata.addImage(WebImage(Uri.parse(coverToUse)))
        }

        val mediaInfo = MediaInfo.Builder(chapter.chapter.audioUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("audio/ogg")
            .setMetadata(movieMetadata)
            .build()

        val loadOptions = MediaLoadOptions.Builder()
            .setAutoplay(autoPlay)
            .setPlayPosition(positionMs)
            .build()

        remoteMediaClient.load(mediaInfo, loadOptions)
    }

    // --- CONTROLES ---

    fun togglePlayPause() {
        if (castSession != null && castSession?.isConnected == true) {
            val remote = castSession?.remoteMediaClient ?: return
            remote.togglePlayback()
        } else {
            mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
        }
    }

    fun seekTo(positionMs: Long) {
        if (castSession != null && castSession?.isConnected == true) {
            // Seek Robusto: Mantém o estado (se estava tocando, continua tocando)
            val seekOptions = MediaSeekOptions.Builder()
                .setPosition(positionMs)
                .setResumeState(MediaSeekOptions.RESUME_STATE_UNCHANGED)
                .build()
            castSession?.remoteMediaClient?.seek(seekOptions)
        } else {
            mediaController?.seekTo(positionMs)
        }
        _uiState.update { it.copy(currentPosition = positionMs) }
    }

    fun fastForward() {
        if (castSession != null && castSession?.isConnected == true) {
            val current = castSession?.remoteMediaClient?.approximateStreamPosition ?: 0
            seekTo(current + 10_000)
        } else {
            mediaController?.let { it.seekTo(it.currentPosition + 10_000) }
        }
    }

    fun rewind() {
        if (castSession != null && castSession?.isConnected == true) {
            val current = castSession?.remoteMediaClient?.approximateStreamPosition ?: 0
            seekTo(maxOf(0, current - 10_000))
        } else {
            mediaController?.let { it.seekTo(it.currentPosition - 10_000) }
        }
    }

    fun onChapterSelected(index: Int) {
        // 1. Atualiza Localmente
        mediaController?.seekTo(index, 0L)
        if (mediaController?.isPlaying == false) mediaController?.play()

        // 2. Atualiza no Cast (SEM DELAY, SEM RACE CONDITION)
        if (castSession?.isConnected == true) {
            val chapter = _uiState.value.chapters.getOrNull(index)
            if (chapter != null) {
                // Manda carregar explicitamente este capítulo
                loadMediaOnCast(chapter, 0L, true)
            }
        }
    }

    fun skipToNextChapter() {
        val nextIndex = _uiState.value.currentChapterIndex + 1
        // Verificação de segurança local
        if (nextIndex < _uiState.value.chapters.size) {
            onChapterSelected(nextIndex)
        }
    }

    fun skipToPreviousChapter() {
        val prevIndex = _uiState.value.currentChapterIndex - 1
        if (prevIndex >= 0) {
            onChapterSelected(prevIndex)
        }
    }

    // --- LOOP PROGRESSO ---
    private fun startProgressLoop() {
        viewModelScope.launch {
            while (isActive) {
                if (castSession != null && castSession?.isConnected == true) {
                    val remote = castSession?.remoteMediaClient
                    if (remote != null && remote.isPlaying) {
                        _uiState.update { it.copy(currentPosition = remote.approximateStreamPosition) }
                    }
                } else {
                    mediaController?.let { player ->
                        if (player.isPlaying) {
                            _uiState.update { it.copy(currentPosition = player.currentPosition) }
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    // --- PADRÃO ---
    fun toggleShuffle() {
        mediaController?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

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

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    // Timer e Conexão Service (Mantidos)
    fun setSleepTimer(minutes: Int) {
        // 1. ATUALIZAÇÃO VISUAL: Marca a opção escolhida no menu imediatamente
        _uiState.update { it.copy(activeSleepTimerMinutes = minutes) }

        // 2. Cancela o job anterior (se houver)
        sleepTimerJob?.cancel()

        // 3. Se escolheu "Desativar" (0), paramos por aqui.
        // O estado já foi atualizado para 0 na linha 1.
        if (minutes <= 0) return

        // 4. Inicia a contagem regressiva
        sleepTimerJob = viewModelScope.launch {
            delay(minutes * 60 * 1000L)

            // O tempo acabou!
            pause()

            // 5. RESET VISUAL: O timer acabou, então desmarcamos a opção no menu (volta para 0)
            _uiState.update { it.copy(activeSleepTimerMinutes = 0) }
            sleepTimerJob = null
        }
    }

    private fun pause() {
        if (_uiState.value.isPlaying) {
            mediaController?.pause()
            if (castSession?.isConnected == true) castSession?.remoteMediaClient?.pause()
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
                if (pendingBookId == "RESUME") {
                    val playingId =
                        mediaController?.currentMediaItem?.mediaMetadata?.extras?.getString("book_id")
                    if (playingId != null) loadBookPlaylist(playingId, "", "")
                    pendingBookId = null
                } else if (pendingBookId != null) {
                    loadBookPlaylist(pendingBookId!!, pendingBookTitle!!, pendingCoverUrl!!)
                    pendingBookId = null; pendingBookTitle = null; pendingCoverUrl = null
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
        if (bookId != "RESUME") _uiState.update {
            it.copy(
                title = initialBookTitle,
                imageUrl = initialCoverUrl
            )
        }
        if (mediaController == null) {
            pendingBookId = bookId; pendingBookTitle = initialBookTitle; pendingCoverUrl =
                initialCoverUrl
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
            if (chapters.isNotEmpty()) setupPlaylist(chapters, bookId)
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
            val metadata = MediaMetadata.Builder().setTitle(chapterInfo.bookName)
                .setSubtitle("Capítulo ${chapterInfo.chapter.number}")
                .setArtworkUri(Uri.parse(chapterInfo.coverUrl)).setExtras(extras).build()
            MediaItem.Builder().setUri(chapterInfo.chapter.audioUrl)
                .setMediaId(chapterInfo.chapter.id.toString()).setMediaMetadata(metadata).build()
        }
        controller.setMediaItems(mediaItems, 0, 0L)
        controller.prepare()
        controller.play()
    }

    private fun updateStateFromPlayer() {
        mediaController?.let { player ->
            _uiState.update { state ->
                state.copy(
                    isPlaying = player.isPlaying,
                    isBuffering = player.playbackState == Player.STATE_BUFFERING,
                    duration = if (player.duration > 0) player.duration else 0L,
                    title = player.currentMediaItem?.mediaMetadata?.title?.toString()
                        ?: state.title,
                    subtitle = player.currentMediaItem?.mediaMetadata?.subtitle?.toString() ?: "",
                    imageUrl = player.currentMediaItem?.mediaMetadata?.artworkUri?.toString()
                        ?: state.imageUrl,
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

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                updateStateFromPlayer()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                updateStateFromPlayer()
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        if (castSession != null) castSession?.remoteMediaClient?.unregisterCallback(castCallback)
        castContext?.sessionManager?.removeSessionManagerListener(
            sessionManagerListener,
            CastSession::class.java
        )
    }
}