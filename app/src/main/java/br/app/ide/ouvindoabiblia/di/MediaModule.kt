package br.app.ide.ouvindoabiblia.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val MIN_BUFFER_MS = 30_000
    private const val MAX_BUFFER_MS = 60_000
    private const val BUFFER_FOR_PLAYBACK_MS = 2_500
    private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000

    /**
     * O ExoPlayer NÃO é mais @Singleton (escopo de processo).
     *
     * Motivo: o player deve pertencer ao PlaybackService — seu ciclo de vida
     * casa com o da MediaLibrarySession, que casa com o do serviço. Como o
     * ExoPlayer só é injetado pelo PlaybackService, um binding sem escopo
     * entrega uma instância nova a cada onCreate do serviço e é liberado uma
     * única vez no onDestroy. Isso elimina o bug crítico em que uma sessão
     * nova era montada sobre um player @Singleton já liberado (DIAGNOSTICO_02 §5.1).
     *
     * Audio focus já está coberto aqui: setAudioAttributes(..., handleAudioFocus = true)
     * + setHandleAudioBecomingNoisy(true) abaixo.
     */
    @OptIn(UnstableApi::class)
    @Provides
    fun provideExoPlayer(
        @ApplicationContext context: Context
    ): ExoPlayer {

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("BibliaFaladaApp")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val errorHandlingPolicy = object : DefaultLoadErrorHandlingPolicy() {
            override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                return 3
            }

            override fun getRetryDelayMsFor(
                loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo
            ): Long {
                val attempt = loadErrorInfo.errorCount.coerceAtMost(3)
                return when (attempt) {
                    1 -> 1_000L
                    2 -> 2_000L
                    else -> 4_000L
                }
            }
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(errorHandlingPolicy)

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(30_000)
            .build()
    }
}