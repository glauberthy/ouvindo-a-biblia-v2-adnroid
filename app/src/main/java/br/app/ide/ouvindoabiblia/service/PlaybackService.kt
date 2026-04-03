package br.app.ide.ouvindoabiblia.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.data.repository.PlaybackState
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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

    @Inject
    lateinit var injectedImageLoader: ImageLoader

    private var mediaSession: MediaLibrarySession? = null

    companion object {
        private const val ROOT_ID = "root_bible"
        private const val TAG = "PlaybackService"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setSessionActivity(getSingleTopActivity())
            .setBitmapLoader(CoilBitmapLoader())
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
        })
    }

//    private fun saveCurrentState() {
//        val currentMediaItem = player.currentMediaItem ?: return
//        val position = player.currentPosition
//        val duration = player.duration
//        val meta = currentMediaItem.mediaMetadata
//
//        serviceScope.launch(Dispatchers.IO) {
//            repository.savePlaybackState(
//                chapterId = currentMediaItem.mediaId,
//                positionMs = position,
//                duration = if (duration > 0) duration else 0L,
//                title = meta.title?.toString() ?: "",
//                subtitle = meta.subtitle?.toString() ?: "",
//                imageUrl = meta.artworkUri?.toString(),
//                audioUrl = currentMediaItem.requestMetadata.mediaUri?.toString() ?: ""
//            )
//        }
//    }

    private fun saveCurrentState() {
        val currentMediaItem = player.currentMediaItem ?: return
        val position = player.currentPosition
        val duration = player.duration
        val meta = currentMediaItem.mediaMetadata

        serviceScope.launch(Dispatchers.IO) {
            repository.savePlaybackState(
                mediaId = currentMediaItem.mediaId, // Agora o Repo aceita a String "study_12" ou "15"
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
        Log.d(TAG, "🔄 RestoreLastSession: INICIANDO...")
        serviceScope.launch(Dispatchers.IO) {
            // 1. Busca do banco. Se retornar algo, É VÁLIDO (garantido pelo SQL).
            val state = repository.getLatestPlaybackState().first()

            if (state != null) {
                // 2. Constrói a playlist usando a função auxiliar
                val result = buildPlaylistFromState(state)

                if (result != null) {
                    withContext(Dispatchers.Main) {
                        if (player.mediaItemCount == 0) {
//                            Log.d(TAG, "✅ Restore: Sucesso! ${state.title}")
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

        // 1. CENÁRIO: RESTORE DE ESTUDOS (mediaId começa com "study_")
        if (mediaId.startsWith("study_")) {
            val studyId = mediaId.removePrefix("study_").toIntOrNull() ?: return null

            // Busca os dados do estudo e suas lições no banco
            val studyData = repository.getStudyWithLessons(studyId).first()

            val playlist = studyData.lessons.map { lesson ->
                MediaItem.Builder()
                    .setMediaId("study_${lesson.remoteId}")
                    .setUri(lesson.url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(studyData.study.title)
                            .setAlbumTitle(studyData.study.title)
                            .setSubtitle(lesson.title)
                            .setArtist("Ouvindo a Bíblia")
                            .setArtworkUri(studyData.study.imageUrl.toUri())
                            .setIsBrowsable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                            .setExtras(Bundle().apply { putString("type", "study") })
                            .build()
                    ).build()
            }

            val startIndex = playlist.indexOfFirst { it.mediaId == mediaId }.coerceAtLeast(0)
            return MediaSession.MediaItemsWithStartPosition(playlist, startIndex, state.positionMs)
        }

        // 2. CENÁRIO: RESTORE DE BÍBLIA (mediaId é apenas um número)
        val chapterIdInt =
            mediaId.toIntOrNull() ?: return null // Converte String para Int com segurança
        val bookNumericId = repository.getBookNumericIdFromChapter(chapterIdInt) ?: return null

        val chapters = repository.getChapters(bookNumericId).first()
        if (chapters.isEmpty()) return null

        val playlist = createMediaItemsFromChapters(chapters, bookNumericId.toString())

        val startIndex = playlist.indexOfFirst {
            it.mediaId == mediaId
        }.coerceAtLeast(0)

        return MediaSession.MediaItemsWithStartPosition(playlist, startIndex, state.positionMs)
    }

//    private fun createMediaItemsFromChapters(
//        chapters: List<ChapterWithBookInfo>,
//        bookId: String
//    ): List<MediaItem> {
//        return chapters.map { chapterInfo ->
//            val metadata = MediaMetadata.Builder()
//                // PARA A NOTIFICAÇÃO: [Livro] [Capítulo]
//                .setTitle("${chapterInfo.bookName} ${chapterInfo.chapter.number}")
//                // PARA O MINI/FULL PLAYER (Linha 1): [Livro]
//                .setAlbumTitle(chapterInfo.bookName)
//                // PARA O MINI/FULL PLAYER (Linha 2): [Capítulo]
//                .setSubtitle("Capítulo ${chapterInfo.chapter.number}")
//                //PARA A NOTIFICAÇÃO (Linha 2)
//                .setArtist("Ouvindo a Bíblia")
//
//                .setArtworkUri(chapterInfo.coverUrl?.toUri())
//                .setIsBrowsable(false)
//                .setIsPlayable(true)
//                .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
//                .setExtras(Bundle().apply {
//                    putString("book_id", bookId)
//                    putBoolean("is_favorite", chapterInfo.chapter.isFavorite)
//                })
//                .build()
//
//            MediaItem.Builder()
//                .setMediaId(chapterInfo.chapter.id.toString())
//                .setUri(chapterInfo.chapter.audioUrl)
//                .setMediaMetadata(metadata)
//                .build()
//        }
//    }

    private fun createMediaItemsFromChapters(
        chapters: List<ChapterWithBookInfo>,
        bookId: String,
        targetChapterIndex: Int = -1, // NOVO: Qual capítulo recebe o recorte?
        clippingConfig: MediaItem.ClippingConfiguration = MediaItem.ClippingConfiguration.UNSET // NOVO: A configuração em si
    ): List<MediaItem> {
        return chapters.mapIndexed { index, chapterInfo ->
            val metadata = MediaMetadata.Builder()
                .setTitle("${chapterInfo.bookName} ${chapterInfo.chapter.number}")
                .setAlbumTitle(chapterInfo.bookName)
                .setSubtitle("Capítulo ${chapterInfo.chapter.number}")
                .setArtist("Ouvindo a Bíblia")
                .setArtworkUri(chapterInfo.coverUrl?.toUri())
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
                .setExtras(Bundle().apply {
                    putString("book_id", bookId)
                    putBoolean("is_favorite", chapterInfo.chapter.isFavorite)
                })
                .build()

            val builder = MediaItem.Builder()
                .setMediaId(chapterInfo.chapter.id.toString())
                .setUri(chapterInfo.chapter.audioUrl)
                .setMediaMetadata(metadata)

            // SINALIZAÇÃO: Aplica o recorte APENAS se for o capítulo que o usuário clicou
            if (index == targetChapterIndex) {
                builder.setClippingConfiguration(clippingConfig)
            }

            builder.build()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
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
        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem != null) {
            saveCurrentState()
        }
        if (!player.playWhenReady || player.playbackState == Player.STATE_IDLE) {
            player.stop()
            player.release()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
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
        @Deprecated("Deprecated in Media3")
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return CallbackToFutureAdapter.getFuture { completer ->
                serviceScope.launch(Dispatchers.Main) {
                    // Cenário 1: Player já vivo
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

                    // Cenário 2: Player morto, reviver do banco
                    withContext(Dispatchers.IO) {
                        try {
                            val state = repository.getLatestPlaybackState().first()

                            // Usa a mesma função mágica do restore!
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
            val isBookFolder = item.mediaMetadata.isBrowsable == true

            if (isBookFolder) {
                val parts = item.mediaId.split("|")
                val bookIdInt = parts[0].toIntOrNull() ?: return super.onSetMediaItems(
                    mediaSession,
                    controller,
                    mediaItems,
                    startIndex,
                    startPositionMs
                )
                val requestedIndex = parts.getOrNull(1)?.toIntOrNull() ?: 0

                // SINALIZAÇÃO: Captura a configuração de recorte que enviamos do ViewModel
                val incomingClippingConfig = item.clippingConfiguration

                return CallbackToFutureAdapter.getFuture { completer ->
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            val chapters = repository.getChapters(bookIdInt).first()

                            if (chapters.isEmpty()) {
                                completer.setException(IllegalStateException("Livro vazio no banco: $bookIdInt"))
                                return@launch
                            }

                            // SINALIZAÇÃO: Passa o índice e o recorte para a fábrica de itens
                            val playlist = createMediaItemsFromChapters(
                                chapters = chapters,
                                bookId = bookIdInt.toString(),
                                targetChapterIndex = requestedIndex,
                                clippingConfig = incomingClippingConfig
                            )

                            completer.set(
                                MediaSession.MediaItemsWithStartPosition(
                                    playlist,
                                    requestedIndex,
                                    0L // Deixe 0L aqui, o ClippingConfig internamente lida com o start!
                                )
                            )
                        } catch (e: Exception) {
                            completer.setException(e)
                        }
                    }
                    "Play Book $bookIdInt"
                }
            }
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