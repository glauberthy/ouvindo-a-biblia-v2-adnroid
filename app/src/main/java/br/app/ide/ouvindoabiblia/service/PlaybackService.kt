package br.app.ide.ouvindoabiblia.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import br.app.ide.ouvindoabiblia.MainActivity
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject
    lateinit var player: ExoPlayer

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

    private fun getSingleTopActivity(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        @UnstableApi
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
        }
    }

    // --- CLASSE INTERNA QUE ENSINA O MEDIA3 A USAR O COIL ---
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
                    .allowHardware(false) // Crucial para notificações

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