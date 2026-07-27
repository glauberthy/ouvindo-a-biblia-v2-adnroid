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
import br.app.ide.ouvindoabiblia.playback.bibleNotificationLine
import br.app.ide.ouvindoabiblia.playback.mediaButtonPreferences
import br.app.ide.ouvindoabiblia.playback.studyNotificationLine
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

    // BUG B (notificação-fantasma): só há sessão de mídia "de verdade" quando o usuário
    // realmente pediu/iniciou playback neste processo. restoreLastSession() carrega uma
    // sessão salva no player só por abrir o app (serviço apenas BOUND, nunca started), o
    // que fazia o Media3 postar uma notificação de mídia não-foreground para conteúdo que
    // o usuário nunca mandou tocar; no swipe o processo morria e a notificação ficava órfã
    // (onTaskRemoved NÃO dispara em serviço só-bound). Este flag distingue "restaurado-
    // nunca-tocado" (não postar) de "tocando/pausado-após-tocar" (postar/manter — 5.1).
    @Volatile
    private var hasStartedPlaybackThisProcess = false

    private fun markExplicitPlaybackRequest(item: MediaItem?) {
        lastExplicitPlaybackRequestAt = SystemClock.elapsedRealtime()
        hasStartedPlaybackThisProcess = true
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

        // Teto para o save síncrono do encerramento (swipe). É um único upsert no
        // Room; o limite existe só para não transformar um disco lento em ANR.
        private const val SHUTDOWN_SAVE_TIMEOUT_MS = 1_500L

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
                // BUG B: qualquer início real de playback (inclusive via botão da
                // notificação/mini-player numa sessão restaurada, que não passa por
                // onSetMediaItems) marca a sessão como ativa — a partir daí a notificação
                // é legítima e deve ser postada/mantida (5.1), mesmo quando pausada.
                if (isPlaying) hasStartedPlaybackThisProcess = true
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


    /**
     * Estado do player capturado de forma SÍNCRONA. Necessário porque no caminho de
     * encerramento (onTaskRemoved) o player é liberado imediatamente depois — ler
     * `player.currentPosition` já dentro da coroutine de IO daria posição de player
     * liberado.
     */
    private data class PlaybackSnapshot(
        val mediaId: String,
        val positionMs: Long,
        val durationMs: Long,
        val title: String,
        val subtitle: String,
        val imageUrl: String?,
        val audioUrl: String
    )

    private suspend fun persist(snapshot: PlaybackSnapshot) {
        repository.savePlaybackState(
            mediaId = snapshot.mediaId,
            positionMs = snapshot.positionMs,
            duration = snapshot.durationMs,
            title = snapshot.title,
            subtitle = snapshot.subtitle,
            imageUrl = snapshot.imageUrl,
            audioUrl = snapshot.audioUrl
        )
    }

    /**
     * Salva a posição BLOQUEANDO a main thread, para o caminho de encerramento.
     *
     * O [saveCurrentState] normal grava no `serviceScope`, que o [onDestroy] cancela
     * (`serviceJob.cancel()`). Como o swipe faz `stopSelf()` na sequência, aquele
     * write seria cancelado antes de commitar no Room e a retomada voltaria ao início
     * do capítulo. Aqui é um único upsert, com teto de tempo para não arriscar ANR.
     */
    private fun saveCurrentStateBlocking() {
        val snapshot = capturePlaybackSnapshot() ?: return
        val saved = runBlocking {
            withTimeoutOrNull(SHUTDOWN_SAVE_TIMEOUT_MS) {
                withContext(Dispatchers.IO) { persist(snapshot) }
                true
            }
        }
        if (saved == null) Log.w(TAG, "saveCurrentStateBlocking: timeout ao salvar no encerramento")
    }

    private fun saveCurrentState() {
        val snapshot = capturePlaybackSnapshot() ?: return
        serviceScope.launch(Dispatchers.IO) { persist(snapshot) }
    }

    private fun capturePlaybackSnapshot(): PlaybackSnapshot? {
        val currentMediaItem = player.currentMediaItem ?: return null
        val mediaId = currentMediaItem.mediaId
        // Só Bíblia e Estudo são persistidos como retomada. Tema (moment) é ignorado
        // de propósito; id malformado é logado e ignorado (ISSUE 2.A/2.C).
        when (MediaContentId.parse(mediaId)) {
            is MediaContentId.Bible, is MediaContentId.Study -> Unit
            is MediaContentId.ThemeMoment -> return null
            null -> {
                Log.w(TAG, "saveCurrentState: mediaId não persistível/malformado, ignorando: $mediaId")
                return null
            }
        }
        // ISSUE 2.D (guarda explícita): num item recortado, player.currentPosition é
        // RELATIVO ao início do recorte, mas buildPlaylistFromState reconstrói SEM
        // recorte e aplicaria a posição como ABSOLUTA (retomada errada). A Bíblia não
        // recorta (params de clipping removidos na 7.A); o recorte vivo é só o de Tema,
        // que não é persistido — a guarda protege esse caso e qualquer recorte futuro.
        if (currentMediaItem.clippingConfiguration != MediaItem.ClippingConfiguration.UNSET) {
            Log.w(TAG, "saveCurrentState: posição de item recortado não persistida (2.D): $mediaId")
            return null
        }
        val duration = player.duration
        val meta = currentMediaItem.mediaMetadata

        return PlaybackSnapshot(
            mediaId = mediaId,
            positionMs = player.currentPosition,
            durationMs = if (duration > 0) duration else 0L,
            title = meta.title?.toString() ?: "",
            subtitle = meta.subtitle?.toString() ?: "",
            imageUrl = meta.artworkUri?.toString(),
            audioUrl = currentMediaItem.requestMetadata.mediaUri?.toString() ?: ""
        )
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
            }
        }
    }

    /**
     * Converte o estado salvo (PlaybackState) em itens tocáveis. Fina camada sobre
     * [buildPlaylistFromMediaId], que é compartilhada com o playback por `mediaId` do
     * Android Auto (`onSetMediaItems`/`onAddMediaItems`/`onGetItem`).
     */
    @OptIn(UnstableApi::class)
    private suspend fun buildPlaylistFromState(state: PlaybackState): MediaSession.MediaItemsWithStartPosition? =
        buildPlaylistFromMediaId(state.mediaId, state.positionMs)

    /**
     * Reconstrói a playlist COMPLETA a partir de um `mediaId` (Bíblia → livro inteiro;
     * Estudo → estudo inteiro), com `startIndex` apontando para o item pedido e a posição
     * dada. Retorna null p/ tema/inválido ou quando o conteúdo não existe mais no banco.
     *
     * É a peça central do playback por `mediaId`: o Android Auto reenvia só o id do item
     * navegado (sem URI), então precisamos remontar aqui os `MediaItem`s tocáveis.
     */
    @OptIn(UnstableApi::class)
    private suspend fun buildPlaylistFromMediaId(
        mediaId: String,
        positionMs: Long,
    ): MediaSession.MediaItemsWithStartPosition? {
        when (val content = MediaContentId.parse(mediaId)) {
            is MediaContentId.Study -> {
                val studyId = content.studyId
                val studyData = repository.getStudyWithLessons(studyId).first()

                val playlist = studyData.lessons.mapIndexed { index, lesson ->
                    MediaItem.Builder()
                        .setMediaId(MediaContentId.Study(studyId, lesson.remoteId).raw)
                        .setUri(lesson.url)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(studyData.title)
                                .setAlbumTitle(studyData.title)
                                .setSubtitle(lesson.title)
                                // ISSUE 10.A: espelha o PlayerViewModel. Este caminho é o da
                                // RETOMADA e do Android Auto — se divergir, a mesma aula
                                // aparece escrita de dois jeitos dependendo de como tocou.
                                .setArtist(
                                    studyNotificationLine(
                                        lessonNumber = index + 1,
                                        totalLessons = studyData.lessons.size,
                                        lessonTitle = lesson.title
                                    )
                                )
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
                    Log.w(TAG, "buildPlaylist abortado: estudo $studyId sem aulas no banco (mediaId=$mediaId)")
                    return null
                }
                val startIndex = playlist.indexOfFirst { it.mediaId == mediaId }.coerceAtLeast(0)
                return MediaSession.MediaItemsWithStartPosition(playlist, startIndex, positionMs)
            }

            is MediaContentId.Bible -> {
                val bookNumericId = repository.getBookNumericIdFromChapter(content.chapterId.toInt())
                if (bookNumericId == null) {
                    Log.w(TAG, "buildPlaylist abortado: livro não encontrado p/ capítulo ${content.chapterId} (mediaId=$mediaId)")
                    return null
                }
                val chapters = repository.getChapters(bookNumericId).first()
                if (chapters.isEmpty()) {
                    Log.w(TAG, "buildPlaylist abortado: livro $bookNumericId sem capítulos no banco (mediaId=$mediaId)")
                    return null
                }
                val playlist = createMediaItemsFromChapters(chapters, bookNumericId.toString())
                val startIndex = playlist.indexOfFirst { it.mediaId == mediaId }.coerceAtLeast(0)
                return MediaSession.MediaItemsWithStartPosition(playlist, startIndex, positionMs)
            }

            // Tema/pasta/inválido: não há playlist a reconstruir.
            else -> {
                Log.w(TAG, "buildPlaylistFromMediaId: mediaId sem playlist (tema/inválido): $mediaId")
                return null
            }
        }
    }

    /**
     * Resolve um `mediaId` no seu `MediaItem` tocável ÚNICO (com URI). Reaproveita
     * [buildPlaylistFromMediaId] e devolve o item no `startIndex` (o próprio item pedido).
     * Usado por `onAddMediaItems`/`onGetItem` (Android Auto) para dar URI a itens que
     * chegam só com o id.
     */
    @OptIn(UnstableApi::class)
    private suspend fun resolvePlayableItem(mediaId: String): MediaItem? {
        val built = buildPlaylistFromMediaId(mediaId, 0L) ?: return null
        return built.mediaItems.getOrNull(built.startIndex)
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
                // ISSUE 10.A: espelha o PlayerViewModel (retomada + Android Auto).
                .setArtist(bibleNotificationLine(chapterInfo.number, chapters.size))
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
        Log.i(LC_TAG, "onUpdateNotification startInForegroundRequired=$startInForegroundRequired hasStarted=$hasStartedPlaybackThisProcess")
        // BUG B (notificação-fantasma): NÃO postar a notificação de mídia de uma sessão
        // só restaurada que o usuário nunca tocou. Esse caso chega SEMPRE com
        // startInForegroundRequired=false (nunca foi promovido a foreground) e com
        // hasStartedPlaybackThisProcess=false. Suprimir só a interseção dos dois preserva:
        //  - tocando (startInForegroundRequired=true) -> posta (foreground, 5.1);
        //  - pausado-após-tocar (hasStarted=true, startInForegroundRequired=false) -> posta/mantém.
        // O flag é obrigatório: gate só por startInForegroundRequired esconderia a
        // notificação legítima do cenário pausado-após-tocar.
        if (!hasStartedPlaybackThisProcess && !startInForegroundRequired) {
            Log.i(LC_TAG, "onUpdateNotification -> SUPRIMIDA (sessão restaurada nunca tocada)")
            return
        }
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
        val wasPlaying = player.isPlaying

        // DECISÃO DE PRODUTO (substitui a Opção A para o caso TOCANDO): remover o app
        // dos recentes é intenção EXPLÍCITA de fechar, então o áudio para e o serviço
        // encerra — como o Spotify. Antes o áudio continuava tocando indefinidamente
        // depois do swipe, sem nenhuma forma de o usuário parar a não ser pela
        // notificação. Mantido da Opção A: no caso PAUSADO-após-tocar a notificação
        // (dismissível) sobrevive, para retomar por ela/headset — ver ISSUE 4.A.
        //
        // NÃO chamamos super.onTaskRemoved(): o default do MediaSessionService faz
        // `if (!isPlaybackOngoing() || !isAnySessionPlaying()) pauseAllPlayersAndStopSelf()`,
        // isto é, encerra só quando NÃO está tocando — exatamente o caso oposto ao que
        // precisamos cobrir aqui. Pular o super é seguro: Service.onTaskRemoved é no-op.
        val decision = decideOnTaskRemoval(wasPlaying, hasStartedPlaybackThisProcess)
        if (decision == TaskRemovalDecision.KEEP_PAUSED_NOTIFICATION) {
            Log.i(LC_TAG, "onTaskRemoved isPlaying=false hasStarted=true -> DECISAO=manter notificação pausada (4.A)")
            saveCurrentState()
            return
        }

        Log.i(LC_TAG, "onTaskRemoved isPlaying=$wasPlaying hasStarted=$hasStartedPlaybackThisProcess -> DECISAO=encerrar (swipe = fechar)")

        // 1) Para o áudio imediatamente e persiste a posição de forma SÍNCRONA: o
        //    stopSelf() abaixo leva ao onDestroy, que cancela o serviceJob — um save
        //    assíncrono aqui seria cancelado antes de commitar (retomada voltaria ao
        //    início do capítulo). capturePlaybackSnapshot trata item nulo e ignora Tema.
        player.pause()
        stopPeriodicSave()
        saveCurrentStateBlocking()

        // 2) Libera sessão + player (ponto único de release fora do onDestroy; zerar
        //    mediaSession evita o double-release, §5.3) e remove a notificação.
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
            // DEFAULT_SESSION_AND_LIBRARY_COMMANDS (não _SESSION_COMMANDS): é o default de um
            // MediaLibrarySession e inclui os comandos de BIBLIOTECA (getLibraryRoot/getChildren/
            // getItem). Usar só DEFAULT_SESSION_COMMANDS negava o browse (PERMISSION_DENIED) para
            // clientes MediaBrowser como o Android Auto (ISSUE 4.D).
            val sessionCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon().build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                // ISSUE 10.B: −10s/+30s nos slots principais das superfícies de mídia
                // (sombra, tela de bloqueio, Auto). Declarado aqui porque vale para TODO
                // cliente que conectar. Não mexer nos comandos acima: a 4.D já ajustou
                // esse conjunto por causa do browse do Android Auto.
                .setMediaButtonPreferences(mediaButtonPreferences())
                .build()
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
                            // Falha de retomada não pode ficar invisível (consistência c/ 2.C).
                            Log.w(TAG, "onPlaybackResumption: falha ao reconstruir playlist do banco", e)
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

            // Android Auto: um toque num item navegado chega como UM item SEM URI
            // (`localConfiguration == null`). Expandimos para a playlist completa
            // (livro/estudo inteiro) com o índice apontando para o item tocado, para
            // dar auto-avanço e next/prev — como o `playBook` do app.
            if (mediaItems.size == 1 && item.localConfiguration == null && item.mediaId.isNotEmpty()) {
                return CallbackToFutureAdapter.getFuture { completer ->
                    serviceScope.launch(Dispatchers.IO) {
                        val expanded = buildPlaylistFromMediaId(item.mediaId, startPositionMs)
                        if (expanded != null) {
                            completer.set(expanded)
                        } else {
                            // Não expansível (tema/inválido): deixa o fluxo padrão resolver.
                            completer.set(
                                MediaSession.MediaItemsWithStartPosition(
                                    mediaItems, startIndex, startPositionMs
                                )
                            )
                        }
                    }
                    "onSetMediaItems"
                }
            }

            // App (telefone): playlists completas já chegam prontas de PlayerViewModel.
            // A expansão de "pasta de livro" (id `{bookId}|{idx}`) foi removida na 3.D.
            return super.onSetMediaItems(
                mediaSession,
                controller,
                mediaItems,
                startIndex,
                startPositionMs
            )
        }

        // Android Auto reenvia itens navegados só com o `mediaId` (sem URI). Aqui
        // resolvemos cada um no seu MediaItem tocável (com URI). Itens que já vêm com
        // URI (do app) passam intactos. É o que faz o playback por `mediaId` funcionar.
        @OptIn(UnstableApi::class)
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            return CallbackToFutureAdapter.getFuture { completer ->
                serviceScope.launch(Dispatchers.IO) {
                    val resolved = mediaItems.mapNotNull { item ->
                        when {
                            item.localConfiguration != null -> item
                            item.mediaId.isNotEmpty() -> resolvePlayableItem(item.mediaId)
                            else -> null
                        }
                    }.toMutableList()
                    completer.set(resolved)
                }
                "onAddMediaItems"
            }
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

        // Android Auto pede o item específico (metadados) por id. Um numérico puro pode
        // ser um LIVRO (nó navegável) ou um CAPÍTULO (tocável) — o namespace numérico é
        // compartilhado (ver MediaContentId) —, então checamos o livro primeiro.
        @OptIn(UnstableApi::class)
        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return CallbackToFutureAdapter.getFuture { completer ->
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        val book = repository.getBooks().first()
                            .firstOrNull { it.numericId.toString() == mediaId }
                        val item = if (book != null) {
                            MediaItem.Builder()
                                .setMediaId(book.numericId.toString())
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(book.name)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS)
                                        .build()
                                ).build()
                        } else {
                            resolvePlayableItem(mediaId)
                        }
                        if (item != null) {
                            completer.set(LibraryResult.ofItem(item, null))
                        } else {
                            completer.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "onGetItem: falha ao resolver mediaId=$mediaId", e)
                        completer.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                    }
                }
                "onGetItem"
            }
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