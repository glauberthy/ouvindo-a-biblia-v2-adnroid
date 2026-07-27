package br.app.ide.ouvindoabiblia.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import br.app.ide.ouvindoabiblia.cast.CastConfig
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Chapter
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Lesson
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Moment
import br.app.ide.ouvindoabiblia.playback.MAX_CAUSE_DEPTH
import br.app.ide.ouvindoabiblia.playback.MediaContentId
import br.app.ide.ouvindoabiblia.playback.bibleNotificationLine
import br.app.ide.ouvindoabiblia.playback.studyNotificationLine
import br.app.ide.ouvindoabiblia.playback.themeMomentNotificationLine
import br.app.ide.ouvindoabiblia.playback.classifyPlaybackError
import br.app.ide.ouvindoabiblia.playback.playbackErrorText
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
    private val repository: BibleRepository // Usado apenas para ações auxiliares (favoritar)
) : ViewModel() {

    private var favoriteObservationJob: Job? = null
    private var studyFavoriteObservationJob: Job? = null

    private var playBookJob: Job? = null

    // --- ESTADO DA UI ---
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // --- CONTROLLER MEDIA3 ---
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // ISSUE 5.B: intenção de play que chegou ANTES do controller conectar (cold start). O
    // buildAsync() leva ~centenas de ms; sem isto, um toque nesse intervalo virava no-op silencioso.
    // Guardamos só a última intenção; é executada no callback de conexão do controllerFuture.
    private var pendingPlayAction: (() -> Unit)? = null

    // Controller pronto para receber comandos de transporte.
    private fun isControllerReady(): Boolean = mediaController?.isConnected == true

    // --- JOBS ---
    private var sleepTimerJob: Job? = null
    private var progressJob: Job? = null
    private var playStudyJob: Job? = null

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

    private var lastTransportActionAt = 0L

    private fun canRunTransportAction(minIntervalMs: Long = 500L): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastTransportActionAt < minIntervalMs) return false
        lastTransportActionAt = now
        return true
    }

    private enum class PlaybackSourceType {
        BIBLE,
        STUDY,
        THEME
    }

    private fun Player.currentSourceType(): PlaybackSourceType {
        return when (MediaContentId.parse(currentMediaItem?.mediaId.orEmpty())) {
            is MediaContentId.Study -> PlaybackSourceType.STUDY
            is MediaContentId.ThemeMoment -> PlaybackSourceType.THEME
            else -> PlaybackSourceType.BIBLE
        }
    }

    private fun forceHardSourceSwitchIfNeeded(
        controller: MediaController,
        targetType: PlaybackSourceType
    ) {
        if (controller.currentSourceType() != targetType) {
            controller.stop()
            controller.clearMediaItems()
        }
    }

    // A Bíblia nunca usa clipping (os capítulos tocam inteiros); o clipping vivo é só o de
    // Tema (ver playThemePlaylist). Por isso este builder não seta ClippingConfiguration.
    private fun buildBibleMediaItems(
        bookId: Int,
        chapters: List<Chapter>
    ): List<MediaItem> {
        return chapters.map { chapterInfo ->
            MediaItem.Builder()
                .setMediaId(MediaContentId.Bible(chapterInfo.id).raw)
                .setUri(chapterInfo.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("${chapterInfo.bookName} ${chapterInfo.number}")
                        .setAlbumTitle(chapterInfo.bookName)
                        .setSubtitle("Capítulo ${chapterInfo.number}")
                        // ISSUE 10.A: `artist` é a 2ª linha da notificação (o `subtitle` não
                        // aparece lá). Total = tamanho da playlist, que aqui é o livro inteiro.
                        .setArtist(bibleNotificationLine(chapterInfo.number, chapters.size))
                        .setArtworkUri(chapterInfo.coverUrl?.toUri())
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
                        .setExtras(android.os.Bundle().apply {
                            putString("book_id", bookId.toString())
                            putBoolean("is_favorite", chapterInfo.isFavorite)
                        })
                        .build()
                )
                .build()
        }
    }

    /**
     * Guard contra replaceMediaItem() redundante.
     *
     * Importante:
     * chamar updatePlayerMetadata() sem mudança real de "is_favorite"
     * pode gerar transitions de playlist desnecessários no Media3
     * e reintroduzir a oscilação entre mídia antiga e nova.
     */
    private fun syncFavoriteMetadataIfNeeded(
        controller: MediaController,
        isFavorite: Boolean
    ) {
        val currentFavorite = controller.currentMediaItem
            ?.mediaMetadata
            ?.extras
            ?.getBoolean("is_favorite", false) == true

        if (currentFavorite != isFavorite) {
            updatePlayerMetadata(controller, isFavorite)
        }
    }

    private var isSourceSwitchInFlight = false
    private var sourceSwitchUnlockJob: Job? = null

    private fun tryBeginSourceSwitch(timeoutMs: Long = 4000L): Boolean {
        if (isSourceSwitchInFlight) return false

        isSourceSwitchInFlight = true
        _uiState.update { it.copy(isSwitchingSource = true) }
        sourceSwitchUnlockJob?.cancel()
        sourceSwitchUnlockJob = viewModelScope.launch {
            delay(timeoutMs)
            // ISSUE 9.C (colateral da 6.G): o timeout precisa liberar TAMBÉM a UI.
            // Antes só resetava o flag interno e uiState.isSwitchingSource ficava
            // preso — controles desabilitados até uma troca de faixa "destravar".
            finishSourceSwitch()
        }
        return true
    }

    private fun finishSourceSwitch() {
        isSourceSwitchInFlight = false
        _uiState.update { it.copy(isSwitchingSource = false) }
        sourceSwitchUnlockJob?.cancel()
        sourceSwitchUnlockJob = null
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
        // Cast fora desta versão — kill-switch único em CastConfig.ENABLED (ISSUE 4.C).
        // Com Cast desligado, não chamamos CastContext.getSharedInstance (some a I/O
        // de disco na main thread flagrada pelo StrictMode no startup/rotação).
        if (CastConfig.ENABLED) initializeCast()

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
                                // ISSUE 9.B: capa 1:1 correta já no mini player restaurado,
                                // antes de o controller conectar.
                                isStudyMode = isStudyMediaId(lastState.mediaId),
                                isPlaying = false, // Cold start é sempre pausado
                                currentPosition = lastState.positionMs,
                                // ISSUE 1.B: se a duração salva for desconhecida (0), NÃO usar 1L.
                                // O getter `progress` já trata `duration <= 0` como 0f (barra vazia);
                                // o fallback antigo (1L, que é > 0) driblava essa guarda e estourava
                                // a fração para 100% (barra cheia falsa) até o controller conectar.
                                duration = if (lastState.duration > 0) lastState.duration else 0L
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

    /**
     * Promove o PlaybackService a started + foreground service.
     *
     * Sem isto o serviço fica apenas BOUND ao MediaController; quando a Activity
     * morre e o controller é liberado (onCleared), o sistema destrói o serviço e
     * o áudio para (DIAGNOSTICO_02 §5.1, regressão observada no moto g53). Como é
     * disparado a partir de uma ação do usuário (app em foreground), o
     * startForegroundService é permitido; o Media3 posta a notificação de mídia
     * (startForeground) assim que a reprodução fica ativa.
     */
    private fun ensureServiceStarted() {
        val intent = Intent(context, PlaybackService::class.java)
        ContextCompat.startForegroundService(context, intent)
        Log.i("PLAYBACK_LC", "startForegroundService chamado (ensureServiceStarted)")
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
                    // BUGFIX restore: no cold start não há EVENT_MEDIA_ITEM_TRANSITION, então o
                    // observer de favorito do item restaurado nunca era ligado e o coração ficava
                    // vazio mesmo favoritado. Liga aqui o observer do item atual.
                    observeFavoriteForCurrentItem()
                }
                // Inicia loop de progresso
                startProgressLoop()

                // ISSUE 5.B: se o usuário tocou em algo antes de o controller conectar, executa
                // agora a intenção guardada (o play explícito sobrepõe a sessão restaurada).
                pendingPlayAction?.let { action ->
                    pendingPlayAction = null
                    action()
                }

            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Falha na conexão com MediaService", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }


    // --- AÇÃO PRINCIPAL: TOCAR TEMA/MOMENTOS ---

    fun playThemePlaylist(
        themeTitle: String,
        themeCoverUrl: String,
        moments: List<Moment>,
        startIndex: Int = 0
    ) {
        // ISSUE 5.B: se o controller ainda não conectou (cold start), guarda a intenção e sai;
        // será reexecutada quando conectar, em vez de virar no-op silencioso.
        if (!isControllerReady()) {
            pendingPlayAction = { playThemePlaylist(themeTitle, themeCoverUrl, moments, startIndex) }
            return
        }
        val controller = mediaController ?: return
        if (!tryBeginSourceSwitch()) return

        // ISSUE 9.C (contexto de Tema) — proteção contra reload, espelhando o ramo do Estudo:
        // se o MESMO tema já está carregado, não recarrega a playlist (antes, cada toque no
        // momento atual refazia setMediaItems e reiniciava o áudio do zero).
        val isPlayingThemeType =
            MediaContentId.parse(controller.currentMediaItem?.mediaId.orEmpty()) is MediaContentId.ThemeMoment
        val isSameTheme =
            controller.currentMediaItem?.mediaMetadata?.albumTitle?.toString() == themeTitle

        if (isPlayingThemeType && isSameTheme && controller.playbackState != Player.STATE_IDLE) {
            if (controller.currentMediaItemIndex != startIndex) {
                // Mesmo tema, momento diferente: pula pra ele (o clipping do item é preservado).
                controller.seekToDefaultPosition(startIndex)
                if (!controller.isPlaying) controller.play()
            } else {
                // Toque no momento ATUAL = pausa/retoma (padrão de mercado).
                if (controller.isPlaying) controller.pause() else controller.play()
            }

            // Nada será recarregado → nenhum evento de playback destrava o guard sozinho.
            finishSourceSwitch()
            return
        }

        _uiState.update { it.copy(title = themeTitle, imageUrl = themeCoverUrl, isStudyMode = false) }

        val themeMediaItems = moments.map { item ->
            val moment = item
            val audioUrl = item.audioUrl
            val bookName = item.bookName
            val coverUrl = item.coverUrl ?: themeCoverUrl

            val clippingConfigBuilder = MediaItem.ClippingConfiguration.Builder()
            if (moment.startMs > 0) {
                clippingConfigBuilder.setStartPositionMs(moment.startMs)
            }
            if (moment.endMs > moment.startMs) {
                clippingConfigBuilder.setEndPositionMs(moment.endMs)
            }

            MediaItem.Builder()
                .setMediaId(MediaContentId.ThemeMoment(moment.id.toString()).raw)
                .setUri(audioUrl)
                .setClippingConfiguration(clippingConfigBuilder.build())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("$bookName ${moment.chapterNumber}")
                        .setAlbumTitle(themeTitle)
                        .setSubtitle("${moment.title} (${moment.reference})")
                        // ISSUE 10.A: sem isto, o momento escolhido pelo usuário não aparecia
                        // em NENHUMA superfície fora do app (o `subtitle` não vai p/ notificação).
                        .setArtist(themeMomentNotificationLine(moment.title, moment.reference))
                        .setArtworkUri(coverUrl.toUri())
                        .setIsBrowsable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .build()
                )
                .build()
        }

        ensureServiceStarted()
        controller.setMediaItems(themeMediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    // 1. A assinatura aceita os tempos do recorte
    fun playBook(
        bookId: Int,
        bookTitle: String,
        coverUrl: String,
        initialIndex: Int = 0
    ) {
        // ISSUE 5.B: adia o play se o controller ainda não conectou (cold start).
        if (!isControllerReady()) {
            pendingPlayAction = { playBook(bookId, bookTitle, coverUrl, initialIndex) }
            return
        }
        val controller = mediaController ?: return
        if (!tryBeginSourceSwitch()) return

        forceHardSourceSwitchIfNeeded(
            controller = controller,
            targetType = PlaybackSourceType.BIBLE
        )

        playBookJob?.cancel()
        playBookJob = viewModelScope.launch {
            val chapters = repository.getChapters(bookId).first()

            if (chapters.isEmpty()) {

                finishSourceSwitch()
                return@launch
            }

            val playlist = buildBibleMediaItems(
                bookId = bookId,
                chapters = chapters
            )

            _uiState.update { it.copy(title = bookTitle, imageUrl = coverUrl, isStudyMode = false) }

            ensureServiceStarted()
            controller.setMediaItems(playlist, initialIndex, 0L)
            controller.prepare()
            controller.play()
        }
    }

    // --- CONTROLES DE MÍDIA ---

    fun togglePlayPause() {
        if (castSession?.isConnected == true) {
            castSession?.remoteMediaClient?.togglePlayback()
        } else {
            mediaController?.let {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    // Se o serviço foi morto enquanto pausado, religa-o antes de tocar.
                    ensureServiceStarted()
                    it.play()
                }
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
        if (!canRunTransportAction()) return
        val controller = mediaController ?: return
        val isPendingPlaylistExpansion =
            controller.mediaItemCount <= 1 &&
                    controller.currentMediaItem?.mediaMetadata?.isBrowsable == true

        if (isPendingPlaylistExpansion) return
        if (!controller.hasNextMediaItem()) return

        controller.seekToNextMediaItem()
    }

    fun skipToPreviousChapter() {
        if (!canRunTransportAction()) return

        val controller = mediaController ?: return

        val isPendingPlaylistExpansion =
            controller.mediaItemCount <= 1 &&
                    controller.currentMediaItem?.mediaMetadata?.isBrowsable == true

        if (isPendingPlaylistExpansion) return
        if (!controller.hasPreviousMediaItem() && controller.currentPosition < 3_000) return

        controller.seekToPreviousMediaItem()
    }

    fun fastForward() {
        mediaController?.seekForward()
        // ISSUE 5.D: limita o progresso otimista pela duração (o rewind já tinha coerceAtLeast(0)).
        // Sem isto, avançar perto do fim estourava a fração > 100% até o loop de progresso corrigir.
        _uiState.update {
            val newPos = it.currentPosition + 30_000
            it.copy(currentPosition = if (it.duration > 0) newPos.coerceAtMost(it.duration) else newPos)
        }
    }

    fun rewind() {
        mediaController?.seekBack()
        _uiState.update {
            it.copy(currentPosition = (it.currentPosition - 10_000).coerceAtLeast(0))
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

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    // ISSUE PUB-02: a UI chama isto após exibir o Toast de erro (padrão consume-once),
    // evitando que o mesmo erro reapareça a cada recomposição.
    fun consumePlaybackError() {
        _uiState.update { it.copy(playbackError = null) }
    }

    fun toggleFavorite() {
        val controller = mediaController ?: return
        val currentIndex = controller.currentMediaItemIndex
        if (currentIndex == -1) return

        val currentItem = controller.currentMediaItem ?: return
        val extras = android.os.Bundle(currentItem.mediaMetadata.extras ?: android.os.Bundle())

        val type = extras.getString("type") ?: "bible"

        // ISSUE 6.G: a fonte de verdade do favorito é o currentIsFavorite do uiState (mantido pelo
        // observador de DB), NÃO o metadata do controller. O extra "is_favorite" do
        // currentMediaItem não reflete os toggles anteriores de forma confiável (o replaceMediaItem
        // não "gruda" na releitura do controller), então lê-lo aqui fazia oldStatus vir sempre
        // false → newStatus sempre true → nunca desfavoritava.
        val oldStatus = _uiState.value.currentIsFavorite
        val newStatus = !oldStatus

        _uiState.update { it.copy(currentIsFavorite = newStatus) }
        updatePlayerMetadata(controller, newStatus)

        viewModelScope.launch {
            runCatching {
                if (type == "study") {
                    val studyId = extras.getInt("study_id")
                    val lessonId = extras.getInt("lesson_id")

                    require(studyId != 0) { "study_id inválido" }
                    require(lessonId != 0) { "lesson_id inválido" }

                    repository.toggleStudyFavorite(
                        studyId = studyId,
                        lessonId = lessonId,
                        isFavorite = newStatus
                    )
                } else {
                    val currentChapter = _uiState.value.chapters.getOrNull(currentIndex)
                        ?: error("Capítulo atual não encontrado")

                    repository.toggleFavorite(
                        chapterId = currentChapter.id,
                        isFavorite = newStatus
                    )
                }
            }.onFailure { error ->
                android.util.Log.e("PlayerViewModel", "Erro ao alternar favorito", error)
                _uiState.update { it.copy(currentIsFavorite = oldStatus) }
                updatePlayerMetadata(controller, oldStatus)
            }
        }
    }

    // Função auxiliar para atualizar o ícone no player sem travar o áudio
    private fun updatePlayerMetadata(
        controller: androidx.media3.session.MediaController,
        newStatus: Boolean
    ) {
        val index = controller.currentMediaItemIndex
        if (index == -1) return

        val item = controller.getMediaItemAt(index)
        val extras = android.os.Bundle(item.mediaMetadata.extras ?: android.os.Bundle()).apply {
            putBoolean("is_favorite", newStatus)
        }

        val newMetadata = item.mediaMetadata.buildUpon()
            .setExtras(extras)
            .build()

        val newItem = item.buildUpon()
            .setMediaMetadata(newMetadata)
            .build()

        controller.replaceMediaItem(index, newItem)
    }

    // --- SINCRONIZAÇÃO DE ESTADO ---

    /**
     * Mensagem por CAUSA do erro. A classificação vive em [classifyPlaybackError]
     * (travada por teste); aqui só extraímos os sinais do PlaybackException.
     */
    private fun playbackErrorMessage(error: PlaybackException?): String {
        val kind = classifyPlaybackError(
            typedHttpCode = typedHttpCodeOf(error),
            messageChain = listOfNotNull(error?.message, error?.cause?.message)
                .joinToString(" "),
            isConnectionError =
                error?.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                        error?.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            isFileNotFound = error?.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
        )
        return playbackErrorText(kind)
    }

    /** Só resolve quando o erro NÃO cruzou IPC; pós-Binder o tipo da causa se perde. */
    private fun typedHttpCodeOf(error: PlaybackException?): Int? {
        var cause: Throwable? = error
        var depth = 0
        while (cause != null && depth++ < MAX_CAUSE_DEPTH) {
            if (cause is InvalidResponseCodeException) return cause.responseCode
            cause = cause.cause
        }
        return null
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_PLAYER_ERROR)) {
                    finishSourceSwitch()
                    // ISSUE PUB-02: o ExoPlayer já re-tentou (ver a política em MediaModule)
                    // antes de emitir o erro. Aqui só damos feedback — a UI mostra um Toast e
                    // chama consumePlaybackError(). Nunca expõe stacktrace.
                    _uiState.update {
                        it.copy(playbackError = playbackErrorMessage(player.playerError))
                    }
                }

                if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                    when (player.playbackState) {
                        Player.STATE_BUFFERING -> Unit

                        Player.STATE_READY,
                        Player.STATE_IDLE,
                        Player.STATE_ENDED -> finishSourceSwitch()
                    }
                }

                syncStateWithController()

                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    // ISSUE 6.G: reflete o favorito do novo item imediatamente e liga o observer
                    // de DB. (Extraído p/ observeFavoriteForCurrentItem para reusar no restore.)
                    observeFavoriteForCurrentItem()
                }
            }
        })
    }

    private fun syncStateWithController() {
        val player = mediaController ?: return

        _uiState.update { state ->
            val currentItem = player.currentMediaItem
            val meta = player.mediaMetadata
            val isTheme = MediaContentId.parse(currentItem?.mediaId.orEmpty()) is MediaContentId.ThemeMoment
            val isStudy = isStudyMediaId(currentItem?.mediaId)

            // ISSUE 6.G: NÃO derivamos currentIsFavorite aqui. Este resync roda a cada evento do
            // player e lê o metadata do controller, que fica ANTIGO enquanto o replaceMediaItem do
            // toggle faz round-trip — sobrescrevia o update otimista e o coração só atualizava
            // depois. O favorito agora é escrito só por: (1) toggleFavorite (otimista), (2) os
            // observadores de DB (observeCurrentFavorite/observeCurrentStudyFavorite) e (3) o
            // handler de EVENT_MEDIA_ITEM_TRANSITION (valor correto no instante da troca).

            state.copy(
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,

                // 1. AJUSTE: Pegamos o AlbumTitle para ser o Título principal da UI (Livro)
                title = meta.albumTitle?.toString() ?: meta.title?.toString() ?: state.title,

                // 2. AJUSTE: Pegamos o Subtitle para ser a segunda linha da UI (Capítulo)
                subtitle = meta.subtitle?.toString() ?: "",

                imageUrl = meta.artworkUri?.toString() ?: state.imageUrl,
                duration = player.duration.coerceAtLeast(0L),
                currentChapterIndex = player.currentMediaItemIndex,
                playbackSpeed = player.playbackParameters.speed,
                chapters = extractChaptersFromPlayer(player),
                timeline = extractTimelineFromPlayer(player),
                isThemeMode = isTheme,
                isStudyMode = isStudy
                // currentIsFavorite intencionalmente NÃO alterado aqui (ISSUE 6.G, ver acima).
            )
        }
    }


    /**
     * Projeção de exibição da timeline para a folha de capítulos (ISSUE 2.B).
     *
     * Por tipo (via [MediaContentId]): Bíblia → número do capítulo (grid de números);
     * Estudo → título da aula, que vive no `subtitle` do MediaItem (lista de títulos).
     * Tema não chega aqui (a folha é escondida em `isThemeMode`), mas cai no ramo de
     * título por segurança.
     */
    private fun extractTimelineFromPlayer(player: Player): List<PlayerTimelineItem> {
        val list = mutableListOf<PlayerTimelineItem>()
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            val meta = item.mediaMetadata
            list.add(
                timelineItemFor(
                    mediaId = item.mediaId,
                    title = meta.title?.toString(),
                    subtitle = meta.subtitle?.toString(),
                    index = i,
                )
            )
        }
        return list
    }

    // Converte a Timeline do Media3 de volta para o modelo de domínio que a UI usa (Chapter)
    private fun extractChaptersFromPlayer(player: Player): List<Chapter> {
        val list = mutableListOf<Chapter>()
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            val meta = item.mediaMetadata

            val chapterId = (MediaContentId.parse(item.mediaId) as? MediaContentId.Bible)?.chapterId ?: 0L
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
            val isFav = meta.extras?.getBoolean("is_favorite") ?: false
            list.add(
                Chapter(
                    id = chapterId,
                    bookId = 0,
                    number = chapterNum,
                    audioUrl = audioUrl,
                    isFavorite = isFav,
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
        chapter: Chapter,
        positionMs: Long,
        autoPlay: Boolean = true
    ) {
        val session = castSession ?: return
        val remote = session.remoteMediaClient ?: return
        val currentState = _uiState.value

        val metadata = CastMediaMetadata(CastMediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
        metadata.putString(CastMediaMetadata.KEY_TITLE, currentState.title)
        metadata.putString(CastMediaMetadata.KEY_SUBTITLE, "Capítulo ${chapter.number}")

        val coverUrl = chapter.coverUrl ?: currentState.imageUrl
        if (coverUrl.isNotEmpty()) {
            metadata.addImage(WebImage(coverUrl.toUri()))
        }

        val mediaInfo = MediaInfo.Builder(chapter.audioUrl)
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
            // ISSUE 5.C: conta TEMPO DE REPRODUÇÃO, não wall-clock. Antes era um único
            // `delay(minutes*60*1000)` que continuava correndo com o áudio pausado e disparava
            // na hora errada. Agora fazemos tick de 1s e só descontamos enquanto está tocando —
            // o cronômetro pausa junto com a reprodução e retoma quando ela volta.
            val tickMs = 1_000L
            var remainingMs = minutes * 60 * 1000L
            while (remainingMs > 0) {
                delay(tickMs)
                if (_uiState.value.isPlaying) remainingMs -= tickMs
            }
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

    /**
     * ISSUE 6.G + restore de sessão: reflete o favorito do item ATUAL no uiState e liga o
     * observador de DB correspondente (Bíblia ou Estudo). Chamado tanto no
     * EVENT_MEDIA_ITEM_TRANSITION quanto no connect do controller (cold start), pois no restore
     * NÃO há transition — sem isto o coração fica vazio mesmo com o item favoritado no banco.
     */
    private fun observeFavoriteForCurrentItem() {
        val player = mediaController ?: return
        val current = player.currentMediaItem ?: return
        val extras = current.mediaMetadata.extras
        val type = extras?.getString("type") ?: "bible"

        // Valor otimista imediato a partir do metadata; o observador de DB abaixo corrige/mantém.
        val currentFavorite = extras?.getBoolean("is_favorite", false) == true
        _uiState.update { it.copy(currentIsFavorite = currentFavorite) }

        if (type == "study") {
            favoriteObservationJob?.cancel()
            val studyId = extras?.getInt("study_id") ?: 0
            val lessonId = extras?.getInt("lesson_id") ?: 0
            if (studyId != 0 && lessonId != 0) {
                observeCurrentStudyFavorite(studyId, lessonId)
            }
        } else {
            studyFavoriteObservationJob?.cancel()
            observeCurrentFavorite(current.mediaId)
        }
    }

    private fun observeCurrentFavorite(chapterId: String?) {
        favoriteObservationJob?.cancel()

        val idLong = chapterId?.toLongOrNull() ?: return

        favoriteObservationJob = viewModelScope.launch {
            repository.getChapterByIdFlow(idLong).collect { chapter ->
                val isFavorite = chapter?.isFavorite ?: false

                _uiState.update { state ->
                    val updatedChapters = state.chapters.map { chapterInfo ->
                        if (chapterInfo.id == idLong) {
                            chapterInfo.copy(isFavorite = isFavorite)
                        } else {
                            chapterInfo
                        }
                    }

                    val currentMediaId = mediaController
                        ?.currentMediaItem
                        ?.mediaId
                        ?.toLongOrNull()

                    state.copy(
                        chapters = updatedChapters,
                        currentIsFavorite = if (currentMediaId == idLong) {
                            isFavorite
                        } else {
                            state.currentIsFavorite
                        }
                    )
                }

                val controller = mediaController
                val currentMediaId = controller?.currentMediaItem?.mediaId?.toLongOrNull()

                if (controller != null && currentMediaId == idLong) {
                    syncFavoriteMetadataIfNeeded(controller, isFavorite)
                }
            }
        }
    }

    // --- AÇÃO PRINCIPAL: TOCAR ESTUDO/AULAS ---

    fun playStudyPlaylist(
        studyTitle: String,
        studyCoverUrl: String,
        lessons: List<Lesson>,
        startIndex: Int = 0
    ) {
        // ISSUE 5.B: adia o play se o controller ainda não conectou (cold start). Cobre também
        // playStudyById, que funila aqui após buscar as aulas.
        if (!isControllerReady()) {
            pendingPlayAction = { playStudyPlaylist(studyTitle, studyCoverUrl, lessons, startIndex) }
            return
        }
        val controller = mediaController ?: return

        // Cobre os dois ramos abaixo (retomar a mesma aula ou carregar nova playlist).
        ensureServiceStarted()

        forceHardSourceSwitchIfNeeded(
            controller = controller,
            targetType = PlaybackSourceType.STUDY
        )

        // --- PROTEÇÃO CONTRA RESTART (Inspirada no seu playBook!) ---
        val currentExtras = controller.currentMediaItem?.mediaMetadata?.extras
        val isPlayingStudyType = currentExtras?.getString("type") == "study"
        val isSameStudy =
            controller.currentMediaItem?.mediaMetadata?.albumTitle?.toString() == studyTitle

        if (isPlayingStudyType && isSameStudy && controller.playbackState != Player.STATE_IDLE) {
            if (controller.currentMediaItemIndex != startIndex) {
                // Mesmo Estudo, aula diferente: pula pra ela (sem recarregar a playlist).
                controller.seekToDefaultPosition(startIndex)
                if (!controller.isPlaying) controller.play()
            } else {
                // ISSUE 9.C: toque na aula ATUAL = pausa/retoma (padrão de mercado).
                // O antigo "replay" era no-op: este ramo nunca reiniciava a aula.
                if (controller.isPlaying) controller.pause() else controller.play()
            }

            // ISSUE 9.C: nada será recarregado, então nenhum evento de playback vai
            // destravar o guard — sem esta chamada, isSwitchingSource ficava preso e a
            // UI congelava em "carregando" (era o bug da seta de replay).
            finishSourceSwitch()
            return // Cancela o recarregamento da playlist!
        }
        // -------------------------------------------------------------

        _uiState.update { it.copy(title = studyTitle, imageUrl = studyCoverUrl, isStudyMode = true) }

        val studyMediaItems = lessons.mapIndexed { index, lesson ->
            MediaItem.Builder()
                .setMediaId(MediaContentId.Study(lesson.studyId, lesson.remoteId).raw)
                .setUri(lesson.url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(studyTitle)
                        .setAlbumTitle(studyTitle)
                        .setSubtitle(lesson.title)
                        // ISSUE 10.A: o `title` é a SÉRIE; sem isto, a notificação não dizia
                        // qual AULA estava tocando. Índice da playlist, não id remoto.
                        .setArtist(studyNotificationLine(index + 1, lessons.size, lesson.title))
                        .setArtworkUri(studyCoverUrl.toUri())
                        .setIsBrowsable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .setExtras(android.os.Bundle().apply {
                            putString("type", "study")
                            putInt("lesson_id", lesson.remoteId)
                            putInt("study_id", lesson.studyId)
                            putBoolean("is_favorite", lesson.isFavorite)
                        })
                        .build()
                )
                .build()
        }
        controller.setMediaItems(studyMediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    // Adicione no seu PlayerViewModel.kt

    fun playStudyById(studyId: Int, title: String, cover: String, startIndex: Int) {
        if (!tryBeginSourceSwitch()) return

        playStudyJob?.cancel()
        playStudyJob = viewModelScope.launch {
            val studyWithLessons = repository.getStudyWithLessons(studyId).first()
            playStudyPlaylist(
                studyTitle = title,
                studyCoverUrl = cover,
                lessons = studyWithLessons.lessons,
                startIndex = startIndex,
            )
        }
    }

    private fun observeCurrentStudyFavorite(studyId: Int, lessonId: Int) {
        studyFavoriteObservationJob?.cancel()

        studyFavoriteObservationJob = viewModelScope.launch {
            repository.getStudyLessonByIdsFlow(studyId, lessonId).collect { lessonEntity ->
                val isFavorite = lessonEntity?.isFavorite ?: false

                _uiState.update { state ->
                    state.copy(currentIsFavorite = isFavorite)
                }

                val controller = mediaController ?: return@collect
                val currentExtras =
                    controller.currentMediaItem?.mediaMetadata?.extras ?: return@collect

                val currentStudyId = currentExtras.getInt("study_id")
                val currentLessonId = currentExtras.getInt("lesson_id")

                if (currentStudyId == studyId && currentLessonId == lessonId) {
                    syncFavoriteMetadataIfNeeded(controller, isFavorite)
                }
            }
        }
    }
}


