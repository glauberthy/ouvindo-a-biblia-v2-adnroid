package br.app.ide.ouvindoabiblia.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.annotation.OptIn
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import br.app.ide.ouvindoabiblia.MainActivity
import br.app.ide.ouvindoabiblia.R
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Chapter
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.data.repository.PlaybackState
import br.app.ide.ouvindoabiblia.playback.MediaContentId
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var repository: BibleRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    // Save periódico de posição enquanto toca (ISSUE 1.A). Sem isto, só salvamos
    // em pause/transição — se o processo morre no meio da faixa, retomamos do
    // início do capítulo. Roda no serviço (não na ViewModel) porque o serviço
    // sobrevive à morte da Activity e cobre a reprodução em background.
    private var periodicSaveJob: Job? = null

    @Inject
    lateinit var injectedImageLoader: ImageLoader

    private var mediaSession: MediaLibrarySession? = null

    // Mantemos a referência para encerrar o threadpool no onDestroy (DIAGNOSTICO_02 §5.4).
    @OptIn(UnstableApi::class)
    private var bitmapLoader: CoilBitmapLoader? = null

    @Volatile
    private var lastExplicitPlaybackRequestAt = 0L

    private fun markExplicitPlaybackRequest(item: MediaItem?) {
        lastExplicitPlaybackRequestAt = SystemClock.elapsedRealtime()
    }

    /**
     * Evita que o PlaybackService restaure estado salvo do banco
     * logo após um comando explícito de reprodução vindo da UI/controller.
     *
     * Isso protege a troca de mídia contra "ressurreição" de sessão anterior
     * durante uma janela curta de transição.
     */
    private fun shouldBlockDatabaseResumption(windowMs: Long = 5_000L): Boolean {
        val now = SystemClock.elapsedRealtime()
        return (now - lastExplicitPlaybackRequestAt) < windowMs
    }


    companion object {
        private const val ROOT_ID = "root_bible"
        private const val TAG = "PlaybackService"
        // TAG única de ciclo de vida para diagnóstico (filtrar por PLAYBACK_LC no Logcat).
        private const val LC_TAG = "PLAYBACK_LC"

        // Intervalo do save periódico de posição (ISSUE 1.A). A tolerância de perda
        // em caso de kill do processo é ≤ este valor.
        private const val PERIODIC_SAVE_INTERVAL_MS = 15_000L

        // Contadores observáveis de ciclo de vida — usados pelo teste instrumentado
        // que trava a regressão do 5.1 (serviço sobrevive ao unbind; player só é
        // liberado no onDestroy). Não têm uso em produção.
        @VisibleForTesting
        val createCount = java.util.concurrent.atomic.AtomicInteger(0)

        @VisibleForTesting
        val destroyCount = java.util.concurrent.atomic.AtomicInteger(0)
    }


    @OptIn(UnstableApi::class)
    override fun onCreate() {

        super.onCreate()

        createCount.incrementAndGet()
        Log.i(LC_TAG, "onCreate")

        val loader = CoilBitmapLoader()
        bitmapLoader = loader

        mediaSession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setSessionActivity(getSingleTopActivity())
            .setBitmapLoader(loader)
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(DefaultMediaNotificationProvider.DEFAULT_CHANNEL_ID)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_notificacao)
        setMediaNotificationProvider(notificationProvider)

        setupAutoSaveListener()
        restoreLastSession()
    }

    private fun setupAutoSaveListener() {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {

                saveCurrentState()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) saveCurrentState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Liga/desliga o save periódico conforme a reprodução real.
                if (isPlaying) startPeriodicSave() else stopPeriodicSave()
            }

        })
    }

    /**
     * Salva a posição em intervalo regular enquanto toca (ISSUE 1.A). Isso garante
     * que, se o processo for morto no meio da faixa, a retomada perca no máximo
     * [PERIODIC_SAVE_INTERVAL_MS] em vez de voltar ao início do capítulo.
     */
    private fun startPeriodicSave() {
        if (periodicSaveJob?.isActive == true) return
        periodicSaveJob = serviceScope.launch {
            while (isActive) {
                delay(PERIODIC_SAVE_INTERVAL_MS)
                saveCurrentState()
            }
        }
    }

    private fun stopPeriodicSave() {
        periodicSaveJob?.cancel()
        periodicSaveJob = null
    }


    private fun saveCurrentState() {
        val currentMediaItem = player.currentMediaItem ?: return
        val mediaId = currentMediaItem.mediaId
        // Só Bíblia e Estudo são persistidos como retomada. Tema (moment) é ignorado
        // de propósito; id malformado é logado e ignorado (ISSUE 2.A/2.C).
        when (MediaContentId.parse(mediaId)) {
            is MediaContentId.Bible, is MediaContentId.Study -> Unit
            is MediaContentId.ThemeMoment -> return
            null -> {
                Log.w(TAG, "saveCurrentState: mediaId não persistível/malformado, ignorando: $mediaId")
                return
            }
        }
        // ISSUE 2.D (guarda explícita): num item recortado, player.currentPosition é
        // RELATIVO ao início do recorte, mas buildPlaylistFromState reconstrói SEM
        // recorte e aplicaria a posição como ABSOLUTA (retomada errada). Hoje o recorte
        // da Bíblia é inalcançável (playBook só recebe startMs=0 dos call-sites vivos),
        // mas a guarda permanece como defesa caso um caminho de recorte seja religado.
        if (currentMediaItem.clippingConfiguration != MediaItem.ClippingConfiguration.UNSET) {
            Log.w(TAG, "saveCurrentState: posição de item recortado não persistida (2.D): $mediaId")
            return
        }
        val position = player.currentPosition
        val duration = player.duration
        val meta = currentMediaItem.mediaMetadata

        serviceScope.launch(Dispatchers.IO) {
            repository.savePlaybackState(
                mediaId = mediaId,
                positionMs = position,
                duration = if (duration > 0) duration else 0L,
                title = meta.title?.toString() ?: "",
                subtitle = meta.subtitle?.toString() ?: "",
                imageUrl = meta.artworkUri?.toString(),
                audioUrl = currentMediaItem.requestMetadata.mediaUri?.toString() ?: ""
            )
        }
    }

    // --- LÓGICA DE RESTORE CORRIGIDA E CENTRALIZADA ---

    @OptIn(UnstableApi::class)
    private fun restoreLastSession() {
        serviceScope.launch(Dispatchers.IO) {
            // 1. Busca do banco. Se retornar algo, É VÁLIDO (garantido pelo SQL).
            val state = repository.getLatestPlaybackState().first()

            if (state != null) {
                // 2. Constrói a playlist usando a função auxiliar
                val result = buildPlaylistFromState(state)

                if (result != null) {
                    withContext(Dispatchers.Main) {
                        if (player.mediaItemCount == 0) {
                            player.setMediaItems(
                                result.mediaItems,
                                result.startIndex,
                                result.startPositionMs
                            )
                            player.prepare()
                            player.playWhenReady = false
                        }
                    }
                }
//                else {
//                    Log.w(TAG, "⚠️ Restore: Falha ao reconstruir playlist (Livro não encontrado?)")
//                }
            }
//            else {
//                Log.d(TAG, "⚠️ Restore: Nada salvo no banco.")
//            }
        }
    }

    /**
     * Função Mágica: Converte o estado salvo (PlaybackState) em itens tocáveis (MediaItems).
     * Retorna null se o livro ou capítulos não existirem mais.
     */
    @OptIn(UnstableApi::class)
    private suspend fun buildPlaylistFromState(state: PlaybackState): MediaSession.MediaItemsWithStartPosition? {
        val mediaId = state.mediaId

        when (val content = MediaContentId.parse(mediaId)) {
            // 1. RESTORE DE ESTUDOS
            is MediaContentId.Study -> {
                val studyId = content.studyId
                val studyData = repository.getStudyWithLessons(studyId).first()

                val playlist = studyData.lessons.map { lesson ->
                    MediaItem.Builder()
                        .setMediaId(MediaContentId.Study(studyId, lesson.remoteId).raw)
                        .setUri(lesson.url)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(studyData.title)
                                .setAlbumTitle(studyData.title)
                                .setSubtitle(lesson.title)
                                .setArtist("Ouvindo a Bíblia")
                                .setArtworkUri(studyData.imageUrl.toUri())
                                .setIsBrowsable(false)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                .setExtras(Bundle().apply {
                                    putString("type", "study")
                                    putInt("study_id", lesson.studyId)
                                    putInt("lesson_id", lesson.remoteId)
                                    putBoolean("is_favorite", lesson.isFavorite)
                                })
                                .build()
                        ).build()
                }

                if (playlist.isEmpty()) {
                    Log.w(TAG, "restore abortado: estudo $studyId sem aulas no banco (mediaId=$mediaId)")
                    return null
                }
                val startIndex = playlist.indexOfFirst { it.mediaId == mediaId }.coerceAtLeast(0)
                return MediaSession.MediaItemsWithStartPosition(playlist, startIndex, state.positionMs)
            }

            // 2. RESTORE DE BÍBLIA
            is MediaContentId.Bible -> {
                val bookNumericId = repository.getBookNumericIdFromChapter(content.chapterId.toInt())
                if (bookNumericId == null) {
                    Log.w(TAG, "restore abortado: livro não encontrado p/ capítulo ${content.chapterId} (mediaId=$mediaId)")
                    return null
                }
                val chapters = repository.getChapters(bookNumericId).first()
                if (chapters.isEmpty()) {
                    Log.w(TAG, "restore abortado: livro $bookNumericId sem capítulos no banco (mediaId=$mediaId)")
                    return null
                }
                val playlist = createMediaItemsFromChapters(chapters, bookNumericId.toString())
                val startIndex = playlist.indexOfFirst { it.mediaId == mediaId }.coerceAtLeast(0)
                return MediaSession.MediaItemsWithStartPosition(playlist, startIndex, state.positionMs)
            }

            // Tema/pasta/inválido: não há retomada a reconstruir.
            else -> {
                Log.w(TAG, "buildPlaylistFromState: mediaId sem restore (tema/inválido): $mediaId")
                return null
            }
        }
    }


    private fun createMediaItemsFromChapters(
        chapters: List<Chapter>,
        bookId: String,
    ): List<MediaItem> {
        return chapters.map { chapterInfo ->
            val metadata = MediaMetadata.Builder()
                .setTitle("${chapterInfo.bookName} ${chapterInfo.number}")
                .setAlbumTitle(chapterInfo.bookName)
                .setSubtitle("Capítulo ${chapterInfo.number}")
                .setArtist("Ouvindo a Bíblia")
                .setArtworkUri(chapterInfo.coverUrl?.toUri())
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
                .setExtras(Bundle().apply {
                    putString("book_id", bookId)
                    putBoolean("is_favorite", chapterInfo.isFavorite)
                })
                .build()

            MediaItem.Builder()
                .setMediaId(MediaContentId.Bible(chapterInfo.id).raw)
                .setUri(chapterInfo.audioUrl)
                .setMediaMetadata(metadata)
                .build()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.i(LC_TAG, "onStartCommand flags=$flags startId=$startId")
        // START_STICKY agora é seguro: o player pertence ao serviço (instância nova
        // por onCreate, ver MediaModule). Se o sistema matar e recriar o serviço,
        // onCreate cria um player novo e restoreLastSession reconstrói a playlist
        // SEM auto-play. Nunca mais se monta sessão sobre player liberado.
        // É também o comportamento desejado no modelo persistente (Spotify-like):
        // deixar o sistema retrazer o serviço de mídia.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i(LC_TAG, "onBind")
        return super.onBind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // Importante: o desbind da Activity NÃO encerra o serviço, porque ele foi
        // INICIADO (startForegroundService) ao começar a tocar. Sobrevive ao unbind.
        Log.i(LC_TAG, "onUnbind")
        return super.onUnbind(intent)
    }

    @OptIn(UnstableApi::class)
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        // Media3 decide aqui se o serviço deve ir/ficar em foreground. Logamos para
        // ver a promoção a foreground (notificação de mídia) no diagnóstico.
        Log.i(LC_TAG, "onUpdateNotification startInForegroundRequired=$startInForegroundRequired")
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    private fun getSingleTopActivity(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            putExtra("OPEN_PLAYER_FROM_NOTIF", true)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Modelo persistente (Opção A): ao remover dos recentes NÃO liberamos o
        // player nem encerramos o serviço — nem tocando, nem pausado.
        //  - Tocando: o áudio continua e a notificação permanece (foreground).
        //  - Pausado: a notificação permanece (dismissível) e o usuário pode dar
        //    play pela notificação/headset ou dispensá-la manualmente com swipe.
        //
        // IMPORTANTE: NÃO chamamos super.onTaskRemoved(). O default do
        // MediaSessionService faz `if (!isPlaybackOngoing() || !isAnySessionPlaying())
        // pauseAllPlayersAndStopSelf()` — ou seja, quando PAUSADO ele dá stopSelf(),
        // o serviço morre e a notificação some (efetivamente a Opção B). Pular o super
        // é seguro: Service.onTaskRemoved (a base) é no-op. O player é liberado
        // exclusivamente no onDestroy real do serviço (DIAGNOSTICO_02 §5.1/§5.2/§5.3).
        // saveCurrentState() já trata currentMediaItem nulo e ignora Tema (moment).
        Log.i(LC_TAG, "onTaskRemoved isPlaying=${player.isPlaying} -> DECISAO=manter (sem super/stopSelf)")
        saveCurrentState()
    }

    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        destroyCount.incrementAndGet()
        val releasePlayer = mediaSession != null
        Log.i(LC_TAG, "onDestroy releasePlayer=$releasePlayer")
        // Único ponto de liberação do player (fim do double-release, §5.3).
        mediaSession?.run {
            Log.i(LC_TAG, "player.release() chamado de onDestroy")
            player.release()
            release()
            mediaSession = null
        }
        // Encerra o threadpool do BitmapLoader para não vazar (§5.4).
        bitmapLoader?.shutdown()
        bitmapLoader = null
        serviceJob.cancel()
        super.onDestroy()
    }

    @UnstableApi
    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands).build()
        }

        // AGORA REUTILIZA A LÓGICA DO RESTORE (DRY)
        // Media3 1.7+: a overload com `isForPlayback` substitui a antiga (2 args), que virou
        // @Deprecated. Semântica do flag:
        //   - false → o sistema só quer METADADOS (ex.: notificação de "continuar" que a UI do
        //     Android monta no boot); NÃO deve iniciar playback.
        //   - true  → devolvemos a playlist + posição e o framework dá play automaticamente.
        // Retornamos o mesmo MediaItemsWithStartPosition nos dois casos; quem decide tocar é o
        // próprio framework a partir do flag, então o comportamento atual é preservado.
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return CallbackToFutureAdapter.getFuture { completer ->
                serviceScope.launch(Dispatchers.Main) {

                    if (shouldBlockDatabaseResumption()) {
                        completer.set(
                            MediaSession.MediaItemsWithStartPosition(
                                emptyList(),
                                0,
                                0L
                            )
                        )
                        return@launch
                    }

                    if (player.mediaItemCount > 0) {
                        val items = mutableListOf<MediaItem>()
                        for (i in 0 until player.mediaItemCount) {
                            items.add(player.getMediaItemAt(i))
                        }


                        completer.set(
                            MediaSession.MediaItemsWithStartPosition(
                                items,
                                player.currentMediaItemIndex,
                                player.currentPosition
                            )
                        )
                        return@launch
                    }

                    withContext(Dispatchers.IO) {
                        try {
                            val state = repository.getLatestPlaybackState().first()


                            val result = if (state != null) buildPlaylistFromState(state) else null

                            if (result != null) {
                                completer.set(result)
                            } else {
                                completer.set(
                                    MediaSession.MediaItemsWithStartPosition(
                                        emptyList(),
                                        0,
                                        0
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            completer.set(
                                MediaSession.MediaItemsWithStartPosition(
                                    emptyList(),
                                    0,
                                    0
                                )
                            )
                        }
                    }
                }
                "onPlaybackResumption"
            }
        }


        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val item = mediaItems.firstOrNull() ?: return super.onSetMediaItems(
                mediaSession,
                controller,
                mediaItems,
                startIndex,
                startPositionMs
            )

            markExplicitPlaybackRequest(item)

            // A expansão de "pasta de livro" (id `{bookId}|{idx}`) foi removida na 3.D
            // junto com o ChaptersScreen morto — nada mais produz esse mediaId. Playlists
            // completas de Bíblia hoje chegam prontas de PlayerViewModel.playBook.
            return super.onSetMediaItems(
                mediaSession,
                controller,
                mediaItems,
                startIndex,
                startPositionMs
            )
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(
                LibraryResult.ofItem(
                    MediaItem.Builder().setMediaId(ROOT_ID).setMediaMetadata(
                        MediaMetadata.Builder().setTitle("Ouvindo a Bíblia").setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED).build()
                    ).build(), params
                )
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return CallbackToFutureAdapter.getFuture { completer ->
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        val children = ImmutableList.builder<MediaItem>()
                        if (parentId == ROOT_ID) {
                            val books = repository.getBooks().first()
                            books.forEach { book ->
                                children.add(
                                    MediaItem.Builder()
                                        .setMediaId(book.numericId.toString()) // USAR O NÚMERO COMO ID DE MÍDIA
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(book.name)
                                                .setIsBrowsable(true)
                                                .setIsPlayable(false)
                                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS)
                                                .build()
                                        ).build()
                                )
                            }
                        } else {
                            // Se o parentId é "6", convertemos para Int e buscamos os capítulos
                            val bookIdInt = parentId.toIntOrNull() ?: 0
                            val chapters = repository.getChapters(bookIdInt).first()
                            children.addAll(createMediaItemsFromChapters(chapters, parentId))
                        }
                        completer.set(LibraryResult.ofItemList(children.build(), params))
                    } catch (e: Exception) {
                        completer.setException(e)
                    }
                }
                "GetChildren"
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED))
        }
    }

    @UnstableApi
    private inner class CoilBitmapLoader : BitmapLoader {
        private val executor = Executors.newCachedThreadPool()

        // Encerra o threadpool quando o serviço é destruído (§5.4).
        fun shutdown() = executor.shutdown()

        override fun supportsMimeType(mimeType: String): Boolean = true
        override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
            return CallbackToFutureAdapter.getFuture { completer ->
                val request =
                    ImageRequest.Builder(this@PlaybackService).data(uri).allowHardware(false)
                        .listener(
                            onSuccess = { _, res -> completer.set(res.drawable.toBitmap()) },
                            onError = { _, res -> completer.setException(res.throwable) }).build()
                injectedImageLoader.enqueue(request)
                "CoilBitmapLoader"
            }
        }

        override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
            return CallbackToFutureAdapter.getFuture { completer ->
                executor.execute {
                    try {
                        completer.set(BitmapFactory.decodeByteArray(data, 0, data.size))
                    } catch (e: Exception) {
                        completer.setException(e)
                    }
                }
                "Decode"
            }
        }

        override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
            return if (metadata.artworkData != null) decodeBitmap(metadata.artworkData!!) else if (metadata.artworkUri != null) loadBitmap(
                metadata.artworkUri!!
            ) else null
        }
    }
}