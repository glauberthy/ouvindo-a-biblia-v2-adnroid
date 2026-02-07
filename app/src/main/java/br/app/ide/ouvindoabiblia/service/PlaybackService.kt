package br.app.ide.ouvindoabiblia.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import android.net.Uri
import androidx.annotation.OptIn
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import br.app.ide.ouvindoabiblia.MainActivity
import br.app.ide.ouvindoabiblia.data.repository.BibleRepository
import br.app.ide.ouvindoabiblia.data.repository.PlaybackState

import coil.ImageLoader
import coil.request.ImageRequest
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var repository: BibleRepository

    // Define o escopo do serviço para coroutines
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    @Inject
    lateinit var injectedImageLoader: ImageLoader

    private var mediaSession: MediaLibrarySession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setSessionActivity(getSingleTopActivity())
            .setBitmapLoader(CoilBitmapLoader())
            .build()

        val notificationProvider = DefaultMediaNotificationProvider(this)
        setMediaNotificationProvider(notificationProvider)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun getSingleTopActivity(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("OPEN_PLAYER_FROM_NOTIF", true)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    // --- NOVO MÉTODO ADICIONADO: DETECTA QUANDO O APP É REMOVIDO DOS RECENTES ---
    // No seu PlaybackService.kt
//    override fun onTaskRemoved(rootIntent: Intent?) {
//        val currentMediaItem = player.currentMediaItem
//        val position = player.currentPosition
//
//        if (currentMediaItem != null) {
//            serviceScope.launch {
//                // Padrão UAMP: Salva o progresso, mas deixa o ciclo de vida para o Android
//                repository.savePlaybackState(currentMediaItem.mediaId, position)
//            }
//        }
//        // Apenas pause. Se o app foi "morto", o Android cuidará de encerrar o processo.
//        player.pause()
//        super.onTaskRemoved(rootIntent)
//    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentMediaItem = player.currentMediaItem
        val position = player.currentPosition

        if (currentMediaItem != null) {
            serviceScope.launch {
                repository.savePlaybackState(currentMediaItem.mediaId, position)
            }
        }
        // Não chame stopSelf() ou player.stop() aqui.
        // Apenas pause para o Android saber que não deve mais manter o áudio ativo.
        player.pause()
        super.onTaskRemoved(rootIntent)
    }
    // --------------------------------------------------------------------------

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        serviceJob.cancel() // Finaliza todas as coroutines do serviço
        super.onDestroy()
    }

    @UnstableApi
    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        @UnstableApi
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
        }


//        override fun onPlaybackResumption(
//            mediaSession: MediaSession,
//            controller: MediaSession.ControllerInfo
//        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
//            return Futures.immediateFailedFuture(UnsupportedOperationException("Not implemented yet"))
//        }

        @OptIn(UnstableApi::class)
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {

            return CallbackToFutureAdapter.getFuture { completer ->
                serviceScope.launch {
                    try {
                        // Forçamos o tipo para evitar o 'Any'
                        val state: PlaybackState? = repository.getLatestPlaybackState().first()

                        if (state != null) {
                            val mediaItem = createMediaItemFromId(state)

                            val result = MediaSession.MediaItemsWithStartPosition(
                                listOf(mediaItem),
                                0,
                                state.positionMs // Agora ele reconhecerá a propriedade
                            )
                            completer.set(result)
                        } else {
                            completer.setException(Exception("Nenhum histórico"))
                        }
                    } catch (e: Exception) {
                        completer.setException(e)
                    }
                }
                "PlaybackResumption"
            }
        }

        private fun createMediaItemFromId(state: PlaybackState): MediaItem {
            val metadata = MediaMetadata.Builder()
                .setTitle(state.title)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build()

            return MediaItem.Builder()
                .setMediaId(state.chapterId)
                .setUri(state.audioUrl) // URL do áudio da Bíblia
                .setMediaMetadata(metadata)
                .build()
        }
    }

    @UnstableApi
    private inner class CoilBitmapLoader : BitmapLoader {

        private val executor = Executors.newCachedThreadPool()

        override fun supportsMimeType(mimeType: String): Boolean {
            return true
        }

        override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
            return CallbackToFutureAdapter.getFuture { completer ->

                val request = ImageRequest.Builder(this@PlaybackService)
                    .data(uri)
                    .allowHardware(false)
                    .listener(
                        onSuccess = { _, result ->
                            completer.set(result.drawable.toBitmap())
                        },
                        onError = { _, result ->
                            completer.setException(result.throwable)
                        }
                    )
                    .build()

                injectedImageLoader.enqueue(request)

                "CoilBitmapLoader: Carregando $uri"
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
                "CoilBitmapLoader: Decodificando bytes"
            }
        }

        override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
            if (metadata.artworkData != null) {
                return decodeBitmap(metadata.artworkData!!)
            }
            if (metadata.artworkUri != null) {
                return loadBitmap(metadata.artworkUri!!)
            }
            return null
        }
    }
}