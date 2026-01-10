package br.app.ide.ouvindoabiblia.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoilModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context
    ): ImageLoader {

        // 1. Criamos o Cliente HTTP EXCLUSIVO para o Coil aqui dentro.
        // Isso evita o conflito "DuplicateBindings" com o NetworkModule.
        val coilOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    // O SEGREDO DO WAF:
                    .addHeader("User-Agent", "BibliaFaladaApp")
                    .build()
                chain.proceed(newRequest)
            }
            .build()

        // 2. Construímos o ImageLoader usando esse cliente privado
        return ImageLoader.Builder(context)
            .okHttpClient(coilOkHttpClient) // Usa o cliente com User-Agent
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}