package br.app.ide.ouvindoabiblia.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context
    ): ExoPlayer {
        // 1. Configura a fonte de dados HTTP com o User-Agent OBRIGATÓRIO
        // Sem isto, o servidor devolve 403 Forbidden
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("BibliaFaladaApp")
            .setAllowCrossProtocolRedirects(true)

        // 2. Configura os atributos de áudio para que o sistema saiba que é reprodução de media
        // (pausa se receber uma chamada, baixa o volume se o GPS falar)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH) // Otimizado para voz/podcast
            .build()

        // 3. Constrói o Player
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(audioAttributes, true) // true = handle audio focus
            .setHandleAudioBecomingNoisy(true) // Pausa se os fones forem desconectados
            .build()
    }
}