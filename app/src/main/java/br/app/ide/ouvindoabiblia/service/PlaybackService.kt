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
import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
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

    // Escopo para operações assíncronas do serviço
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    @Inject
    lateinit var injectedImageLoader: ImageLoader

    private var mediaSession: MediaLibrarySession? = null

    companion object {
        private const val ROOT_ID = "root_bible"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setSessionActivity(getSingleTopActivity())
            .setBitmapLoader(CoilBitmapLoader())
            .build()

        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))

        setupAutoSaveListener()
        restoreLastSession()
    }


    private fun setupAutoSaveListener() {
        player.addListener(object : Player.Listener {
            // Salva assim que mudar de música/capítulo
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                saveCurrentState()
            }

            // Salva assim que PAUSAR. Garantia de ferro.
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) { // Se pausou...
                    saveCurrentState()
                }
            }
        })
    }

    // Função que faz o trabalho sujo de salvar
    private fun saveCurrentState() {
        val currentMediaItem = player.currentMediaItem ?: return
        val position = player.currentPosition
        val duration = player.duration
        val meta = currentMediaItem.mediaMetadata

        serviceScope.launch(Dispatchers.IO) {
            repository.savePlaybackState(
                chapterId = currentMediaItem.mediaId,
                positionMs = position,
                duration = if (duration > 0) duration else 0L,
                title = meta.title?.toString() ?: "",
                subtitle = meta.subtitle?.toString() ?: "",
                imageUrl = meta.artworkUri?.toString(),
                audioUrl = currentMediaItem.requestMetadata.mediaUri?.toString() ?: ""
            )
        }
    }

    private fun restoreLastSession() {
        Log.d("PlaybackService", "🔄 RestoreLastSession: INICIANDO...")

        serviceScope.launch(Dispatchers.IO) {
            val state = repository.getLatestPlaybackState().first()
            if (state == null) {
                Log.d("PlaybackService", "⚠️ Restore: Nenhum estado salvo.")
                return@launch
            }

            // Tenta achar o ID do livro
            val bookId = repository.getBookIdFromChapter(state.chapterId)

            // --- AQUI É O PULO DO GATO ---
            if (bookId == null) {
                Log.e(
                    "PlaybackService",
                    "❌ Restore: BookId não encontrado para Cap ${state.chapterId}. Limpando lixo..."
                )

                // Limpa o estado podre para parar de dar erro nas próximas vezes
                repository.clearPlaybackState()

                return@launch
            }
            // -----------------------------

            val chapters = repository.getChapters(bookId).first()
            if (chapters.isEmpty()) {
                Log.e("PlaybackService", "❌ Restore: Capítulos vazios. Limpando...")
                repository.clearPlaybackState() // Opcional: limpar aqui também
                return@launch
            }

            // ... (Resto do código de carregar a playlist continua igual) ...
            val playlist = createMediaItemsFromChapters(chapters, bookId)
            val startIndex =
                playlist.indexOfFirst { it.mediaId == state.chapterId }.coerceAtLeast(0)

            withContext(Dispatchers.Main) {
                if (player.mediaItemCount > 0) return@withContext
                player.setMediaItems(playlist, startIndex, state.positionMs)
                player.prepare()
                player.playWhenReady = false
                Log.d("PlaybackService", "✅ Restore: Sucesso!")
            }
        }
    }

    // --- FUNÇÃO AUXILIAR (Nova) ---
    // Centraliza a lógica de criação de itens para garantir consistência de metadados
    private fun createMediaItemsFromChapters(
        chapters: List<ChapterWithBookInfo>,
        bookId: String
    ): List<MediaItem> {
        return chapters.map { chapterInfo ->
            val metadata = MediaMetadata.Builder()
                // Título: "Gênesis 1"
                .setTitle("${chapterInfo.bookName} ${chapterInfo.chapter.number}")
                // Subtítulo: "Capítulo 1"
                .setSubtitle("Capítulo ${chapterInfo.chapter.number}")
                // Artista: Marca do App
                .setArtist("Ouvindo a Bíblia")
                .setArtworkUri(chapterInfo.coverUrl?.toUri())
                .setAlbumTitle(chapterInfo.bookName)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
                .setExtras(Bundle().apply {
                    putString("book_id", bookId)
                })
                .build()

            MediaItem.Builder()
                .setMediaId(chapterInfo.chapter.id.toString())
                .setUri(chapterInfo.chapter.audioUrl)
                .setMediaMetadata(metadata)
                .build()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)
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
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentMediaItem = player.currentMediaItem
        val position = player.currentPosition
        val duration = player.duration

        if (currentMediaItem != null) {
            val meta = currentMediaItem.mediaMetadata
            serviceScope.launch {
                repository.savePlaybackState(
                    chapterId = currentMediaItem.mediaId,
                    positionMs = position,
                    duration = if (duration > 0) duration else 0L,
                    title = meta.title?.toString() ?: "",
                    subtitle = meta.subtitle?.toString() ?: "",
                    imageUrl = meta.artworkUri?.toString(),
                    audioUrl = currentMediaItem.requestMetadata.mediaUri?.toString() ?: ""
                )
            }
        }

        if (!player.playWhenReady || player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
            player.stop()
            player.release()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release() // Agora está CORRETO. O Serviço criou, o Serviço mata.
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
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        @Deprecated("Deprecated in Media3")
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {

            return CallbackToFutureAdapter.getFuture { completer ->
                serviceScope.launch(Dispatchers.Main) {
                    // CENÁRIO 1: O player já foi restaurado pelo nosso 'restoreLastSession' e tem itens.
                    if (player.mediaItemCount > 0) {
                        val items = mutableListOf<MediaItem>()
                        for (i in 0 until player.mediaItemCount) {
                            items.add(player.getMediaItemAt(i))
                        }
                        val result = MediaSession.MediaItemsWithStartPosition(
                            items,
                            player.currentMediaItemIndex,
                            player.currentPosition
                        )
                        completer.set(result)
                        return@launch
                    }

                    withContext(Dispatchers.IO) {
                        try {
                            val state = repository.getLatestPlaybackState().first()

                            // Se falhar em qualquer etapa, retornamos uma lista vazia
                            // Isso evita o UnsupportedOperationException e o app apenas inicia sem tocar nada.
                            if (state == null) {
                                completer.set(
                                    MediaSession.MediaItemsWithStartPosition(
                                        emptyList(),
                                        0,
                                        0
                                    )
                                )
                                return@withContext
                            }

                            val bookId = repository.getBookIdFromChapter(state.chapterId)
                            if (bookId == null) {
                                // Log para debug: Log.w("Service", "Livro não encontrado para cap: ${state.chapterId}")
                                completer.set(
                                    MediaSession.MediaItemsWithStartPosition(
                                        emptyList(),
                                        0,
                                        0
                                    )
                                )
                                return@withContext
                            }

                            val chapters = repository.getChapters(bookId).first()
                            if (chapters.isEmpty()) {
                                completer.set(
                                    MediaSession.MediaItemsWithStartPosition(
                                        emptyList(),
                                        0,
                                        0
                                    )
                                )
                                return@withContext
                            }

                            // Sucesso!
                            val playlist = createMediaItemsFromChapters(chapters, bookId)
                            val startIndex = playlist.indexOfFirst { it.mediaId == state.chapterId }
                                .coerceAtLeast(0)

                            val result = MediaSession.MediaItemsWithStartPosition(
                                playlist,
                                startIndex,
                                state.positionMs
                            )
                            completer.set(result)

                        } catch (e: Exception) {
                            // Em caso de erro, falha graciosa
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

        // --- MUDANÇA: Play Folder usando a função auxiliar ---
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {

            val item = mediaItems.firstOrNull()
            val isBookFolder = item?.mediaMetadata?.isBrowsable == true

            if (mediaItems.size == 1 && isBookFolder) {
                val bookId = item.mediaId

                return CallbackToFutureAdapter.getFuture { completer ->
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            val chapters = repository.getChapters(bookId).first()

                            if (chapters.isEmpty()) {
                                completer.setException(IllegalStateException("Livro vazio: $bookId"))
                                return@launch
                            }

                            // Reusa a função para garantir os mesmos metadados do restore
                            val playlist = createMediaItemsFromChapters(chapters, bookId)

                            val result = MediaSession.MediaItemsWithStartPosition(
                                playlist,
                                0,
                                0
                            )
                            completer.set(result)
                        } catch (e: Exception) {
                            completer.setException(e)
                        }
                    }
                    "Carregando Playlist do Livro $bookId"
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
            val rootExtras = Bundle().apply {
                putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
                putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 2)
                putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1)
            }

            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Ouvindo a Bíblia")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setExtras(rootExtras)
                        .build()
                )
                .build()

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
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
                                        .setMediaId(book.bookId)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(book.name)
                                                .setSubtitle("${book.totalChapters} Capítulos")
                                                .setArtworkUri(book.imageUrl?.toUri())
                                                .setIsBrowsable(true)
                                                .setIsPlayable(false)
                                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS)
                                                .build()
                                        )
                                        .build()
                                )
                            }
                        } else {
                            // Retorna capítulos como itens navegáveis para Android Auto
                            val chapters = repository.getChapters(parentId).first()
                            // Reusa a função para consistência
                            val items = createMediaItemsFromChapters(chapters, parentId)
                            children.addAll(items)
                        }
                        completer.set(LibraryResult.ofItemList(children.build(), params))
                    } catch (e: Exception) {
                        completer.setException(e)
                    }
                }
                "Carregando filhos de $parentId"
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
                val request = ImageRequest.Builder(this@PlaybackService)
                    .data(uri)
                    .allowHardware(false)
                    .listener(
                        onSuccess = { _, result -> completer.set(result.drawable.toBitmap()) },
                        onError = { _, result -> completer.setException(result.throwable) }
                    )
                    .build()
                injectedImageLoader.enqueue(request)
                "CoilBitmapLoader: $uri"
            }
        }

        override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
            return CallbackToFutureAdapter.getFuture { completer ->
                executor.execute {
                    try {
                        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                        completer.set(bitmap)
                    } catch (e: Exception) {
                        completer.setException(e)
                    }
                }
                "CoilBitmapLoader: Decode"
            }
        }

        override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
            if (metadata.artworkData != null) return decodeBitmap(metadata.artworkData!!)
            if (metadata.artworkUri != null) return loadBitmap(metadata.artworkUri!!)
            return null
        }
    }
}