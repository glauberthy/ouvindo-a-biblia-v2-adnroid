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
import br.app.ide.ouvindoabiblia.data.local.entity.ChapterEntity
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.android.gms.cast.MediaMetadata as CastMediaMetadata

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BibleRepository // Usado apenas para ações auxiliares (favoritar)
) : ViewModel() {

    // --- ESTADO DA UI ---
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // --- CONTROLLER MEDIA3 ---
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // --- JOBS ---
    private var sleepTimerJob: Job? = null
    private var progressJob: Job? = null

    // --- CAST VARS ---
    private var castContext: CastContext? = null
    private var castSession: CastSession? = null

    // --- CAST LISTENERS ---
    private val castCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            updateStateFromCast()
            checkCastCompletion()
        }

        override fun onMetadataUpdated() {
            updateStateFromCast()
        }
    }

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            castSession = session
            session.remoteMediaClient?.registerCallback(castCallback)

            // Sincroniza o capítulo atual para o Cast
            val currentChapter =
                _uiState.value.chapters.getOrNull(_uiState.value.currentChapterIndex)
            if (currentChapter != null) {
                loadMediaOnCast(currentChapter, _uiState.value.currentPosition)
            }
            mediaController?.pause() // Pausa o local
            updateStateFromCast()
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionEnding(session: CastSession) {
            session.remoteMediaClient?.unregisterCallback(castCallback)
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            castSession = null
            // Ao desconectar, podemos forçar uma atualização do estado local
            syncStateWithController()
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

    init {
        initializeCast()

        // --- INICIALIZAÇÃO OTIMIZADA COM ROOM ---
        viewModelScope.launch {
            // Observa continuamente o estado do banco.
            // Se o banco mudar (ex: limpeza de dados), a UI reflete isso.
            repository.getLatestPlaybackState().collect { lastState ->

                // CRÍTICO: Só atualizamos a UI baseada no banco SE o MediaController
                // ainda não estiver conectado. Assim que o Controller conectar, ELE manda na UI.
                if (mediaController == null || mediaController?.isConnected == false) {

                    if (lastState != null && lastState.title.isNotEmpty()) {
                        // Tem histórico válido: Mostra o Mini Player / Botão Continuar
                        _uiState.update {
                            it.copy(
                                title = lastState.title,
                                subtitle = lastState.subtitle.ifEmpty { "Continuar Ouvindo" },
                                imageUrl = lastState.imageUrl ?: "",
                                isPlaying = false, // Cold start é sempre pausado
                                currentPosition = lastState.positionMs,
                                duration = if (lastState.duration > 0) lastState.duration else 1L
                            )
                        }
                    } else {
                        // Banco vazio ou inválido: Limpa a UI para esconder o player
                        _uiState.update { it.copy(title = "") }
                    }
                }
            }
        }

        initializeController()
    }

    private fun initializeCast() {
        try {
            castContext = CastContext.getSharedInstance(context)
            castContext?.sessionManager?.addSessionManagerListener(
                sessionManagerListener,
                CastSession::class.java
            )
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Erro ao inicializar Cast: ${e.message}")
        }
    }

    private fun initializeController() {
        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()

                if ((mediaController?.mediaItemCount ?: 0) > 0) {
                    syncStateWithController()
                }
                // Inicia loop de progresso
                startProgressLoop()

            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Falha na conexão com MediaService", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // --- AÇÃO PRINCIPAL: TOCAR LIVRO ---
    fun playBook(bookId: String, bookTitle: String, coverUrl: String) {
        val controller = mediaController ?: return

        // Evita recarregar se já estivermos tocando este livro
        val currentBookId = controller.currentMediaItem?.mediaMetadata?.extras?.getString("book_id")
        if (currentBookId == bookId && controller.playbackState != Player.STATE_IDLE) {
            if (!controller.isPlaying) controller.play()
            return
        }

        // Atualiza UI otimista
        _uiState.update { it.copy(title = bookTitle, imageUrl = coverUrl) }

        // Cria o MediaItem "Pasta" que o Serviço vai interceptar
        val bookFolderItem = MediaItem.Builder()
            .setMediaId(bookId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(bookTitle)
                    .setArtworkUri(coverUrl.toUri())
                    .setIsBrowsable(true) // Sinaliza que é uma pasta/livro
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS)
                    .build()
            )
            .build()

        // Envia para o Serviço (ele vai buscar no Room e montar a playlist)
        controller.setMediaItems(listOf(bookFolderItem))
        controller.prepare()
        controller.play()
    }

    // --- CONTROLES DE MÍDIA ---

    fun togglePlayPause() {
        if (castSession?.isConnected == true) {
            castSession?.remoteMediaClient?.togglePlayback()
        } else {
            mediaController?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        if (castSession?.isConnected == true) {
            val seekOptions = MediaSeekOptions.Builder()
                .setPosition(positionMs)
                .setResumeState(MediaSeekOptions.RESUME_STATE_UNCHANGED)
                .build()
            castSession?.remoteMediaClient?.seek(seekOptions)
        } else {
            mediaController?.seekTo(positionMs)
        }
        // Feedback visual imediato
        _uiState.update { it.copy(currentPosition = positionMs) }
    }

    fun skipToNextChapter() {
        if (castSession?.isConnected == true) {
            val nextIndex = _uiState.value.currentChapterIndex + 1
            if (nextIndex < _uiState.value.chapters.size) {
                onChapterSelected(nextIndex)
            }
        } else {
            mediaController?.seekToNextMediaItem()
        }
    }

    fun skipToPreviousChapter() {
        if (castSession?.isConnected == true) {
            val prevIndex = _uiState.value.currentChapterIndex - 1
            if (prevIndex >= 0) onChapterSelected(prevIndex)
        } else {
            mediaController?.seekToPreviousMediaItem()
        }
    }

    fun onChapterSelected(index: Int) {
        // Local
        mediaController?.seekTo(index, 0L)
        mediaController?.play()

        // Cast
        if (castSession?.isConnected == true) {
            val chapter = _uiState.value.chapters.getOrNull(index)
            if (chapter != null) {
                loadMediaOnCast(chapter, 0L, true)
            }
        }
    }

    fun toggleShuffle() {
        mediaController?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun toggleFavorite() {
        val currentChapter =
            _uiState.value.chapters.getOrNull(_uiState.value.currentChapterIndex) ?: return
        viewModelScope.launch {
            repository.toggleFavorite(currentChapter.chapter.id, !currentChapter.chapter.isFavorite)
        }
    }

    // --- SINCRONIZAÇÃO DE ESTADO ---

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                syncStateWithController()
            }
        })
    }

    private fun syncStateWithController() {
        val player = mediaController ?: return

        _uiState.update { state ->
            state.copy(
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                title = player.mediaMetadata.title?.toString() ?: state.title,
                subtitle = player.mediaMetadata.subtitle?.toString() ?: "",
                artist = player.mediaMetadata.artist?.toString() ?: "Ouvindo a Bíblia",
                imageUrl = player.mediaMetadata.artworkUri?.toString() ?: state.imageUrl,
                duration = player.duration.coerceAtLeast(0L),
                currentChapterIndex = player.currentMediaItemIndex,
                playbackSpeed = player.playbackParameters.speed,
                isShuffleEnabled = player.shuffleModeEnabled,
                chapters = extractChaptersFromPlayer(player)
            )
        }
    }


    fun fastForward() {
        if (castSession?.isConnected == true) {
            val current = castSession?.remoteMediaClient?.approximateStreamPosition ?: 0
            seekTo(current + 30_000) // Avança 30s no Cast
        } else {
            mediaController?.seekForward()
            _uiState.update { it.copy(currentPosition = it.currentPosition + 30_000) }
        }
    }

    fun rewind() {
        if (castSession?.isConnected == true) {
            val current = castSession?.remoteMediaClient?.approximateStreamPosition ?: 0
            seekTo((current - 10_000).coerceAtLeast(0)) // Volta 10s no Cast
        } else {
            mediaController?.seekBack()
            _uiState.update {
                it.copy(currentPosition = (it.currentPosition - 10_000).coerceAtLeast(0))
            }
        }
    }

    // Converte a Timeline do Media3 de volta para o modelo que sua UI usa (ChapterWithBookInfo)
    private fun extractChaptersFromPlayer(player: Player): List<ChapterWithBookInfo> {
        val list = mutableListOf<ChapterWithBookInfo>()
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            val meta = item.mediaMetadata

            val chapterId = item.mediaId.toLongOrNull() ?: 0L
            val titleStr = meta.title?.toString() ?: ""

            // Extração segura do número do capítulo
            val chapterNum = try {
                titleStr.trim().split(" ").last().toInt()
            } catch (e: NumberFormatException) {
                i + 1
            }

            val bookName = meta.albumTitle?.toString() ?: meta.subtitle?.toString() ?: ""
            val audioUrl = item.requestMetadata.mediaUri?.toString() ?: ""
            val coverUrl = meta.artworkUri?.toString()

            list.add(
                ChapterWithBookInfo(
                    chapter = ChapterEntity(
                        id = chapterId,
                        bookId = "",
                        number = chapterNum,
                        audioUrl = audioUrl,
                        filename = ""
                    ),
                    bookName = bookName,
                    coverUrl = coverUrl
                )
            )
        }
        return list
    }

    // --- LOOP E UTILITÁRIOS ---

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                if (castSession?.isConnected == true) {
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

    // --- CAST LOGIC ---

    private fun updateStateFromCast() {
        val session = castSession ?: return
        if (!session.isConnected) return
        val remote = session.remoteMediaClient ?: return

        _uiState.update {
            it.copy(
                isPlaying = remote.isPlaying,
                duration = if (remote.streamDuration > 0) remote.streamDuration else it.duration,
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

    private fun loadMediaOnCast(
        chapter: ChapterWithBookInfo,
        positionMs: Long,
        autoPlay: Boolean = true
    ) {
        val session = castSession ?: return
        val remote = session.remoteMediaClient ?: return
        val currentState = _uiState.value

        val metadata = CastMediaMetadata(CastMediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
        metadata.putString(CastMediaMetadata.KEY_TITLE, currentState.title)
        metadata.putString(CastMediaMetadata.KEY_SUBTITLE, "Capítulo ${chapter.chapter.number}")

        val coverUrl = chapter.coverUrl ?: currentState.imageUrl
        if (coverUrl.isNotEmpty()) {
            metadata.addImage(WebImage(coverUrl.toUri()))
        }

        val mediaInfo = MediaInfo.Builder(chapter.chapter.audioUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("audio/ogg")
            .setMetadata(metadata)
            .build()

        val options = MediaLoadOptions.Builder()
            .setAutoplay(autoPlay)
            .setPlayPosition(positionMs)
            .build()

        remote.load(mediaInfo, options)
    }

    // --- SLEEP TIMER ---

    fun setSleepTimer(minutes: Int) {
        _uiState.update { it.copy(activeSleepTimerMinutes = minutes) }
        sleepTimerJob?.cancel()

        if (minutes <= 0) return

        sleepTimerJob = viewModelScope.launch {
            delay(minutes * 60 * 1000L)
            if (castSession?.isConnected == true) {
                castSession?.remoteMediaClient?.pause()
            } else {
                mediaController?.pause()
            }
            _uiState.update { it.copy(activeSleepTimerMinutes = 0, isPlaying = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let { MediaController.releaseFuture(it) }

        if (castSession != null) {
            castSession?.remoteMediaClient?.unregisterCallback(castCallback)
        }
        castContext?.sessionManager?.removeSessionManagerListener(
            sessionManagerListener,
            CastSession::class.java
        )
    }
}