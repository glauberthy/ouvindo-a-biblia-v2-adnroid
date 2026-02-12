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

    @OptIn(UnstableApi::class)
    @Provides
//    @Singleton
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
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        // BUFFER: LoadControl "Freio ABS" (Evita Rate Limit do Cloudflare)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000, // Min buffer: 30s (garante fluidez)
                60_000, // Max buffer: 60s (evita baixar dados demais se o user pular)
                2_500,  // Buffer inicial para tocar
                5_000   // Buffer para retomar após travamento
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // RESILIÊNCIA: Política de Retry Automático
        // Se a internet cair, tenta reconectar agressivamente para áudio
        val errorHandlingPolicy = object : DefaultLoadErrorHandlingPolicy() {
            override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                return Int.MAX_VALUE // Tenta infinitamente se for erro de conexão
            }

            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                // Backoff suave para não bombardear o servidor
                return super.getRetryDelayMsFor(loadErrorInfo).coerceAtMost(5000)
            }
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(errorHandlingPolicy)

        // CONSTRUÇÃO FINAL DO PLAYER
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true) // Handle Audio Focus (ligações)
            .setHandleAudioBecomingNoisy(true)         // Pausa se fone desconectar
            .setWakeMode(C.WAKE_MODE_NETWORK)          // CRÍTICO: Mantém CPU/Wifi acordados
            .setSeekBackIncrementMs(10_000)            // Padrão de podcast (10s)
            .setSeekForwardIncrementMs(30_000)         // Padrão de podcast (30s)
            .build()
    }
}